package dev.crashteam.uzumanalytics.job

import dev.crashteam.uzumanalytics.client.uzum.UzumClient
import dev.crashteam.uzumanalytics.client.uzum.model.CategoryData
import dev.crashteam.uzumanalytics.client.uzum.model.SimpleCategory
import dev.crashteam.uzumanalytics.config.properties.UzumProperties
import dev.crashteam.uzumanalytics.extensions.getApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.quartz.*
import org.quartz.impl.matchers.GroupMatcher
import org.springframework.retry.support.RetryTemplate
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean
import java.util.*
import kotlin.random.Random

private val log = KotlinLogging.logger {}

@DisallowConcurrentExecution
class PositionProductMasterJob : Job {

    override fun execute(jobContext: JobExecutionContext) {
        val applicationContext = jobContext.getApplicationContext()
        val uzumClient = applicationContext.getBean(UzumClient::class.java)
        val rootCategories = uzumClient.getRootCategories()
            ?: throw IllegalStateException("Can't get root categories")
        val firstCategory = rootCategories.payload.first()
        val categoryGQL = uzumClient.getCategoryGQL(firstCategory.id.toString(), 0, 0)?.data?.makeSearch
            ?: throw IllegalStateException("Can't get categories")
        val categoryIds = categoryGQL.categoryTree.map {
            it.category.id
        }
        log.info { "Loop categories for product position job. categories count - ${categoryIds.size}" }
        runBlocking {
            for (categoryId in categoryIds) {
                val jobIdentity = "${categoryId}-position-product-job"
                val jobKey = JobKey(jobIdentity)
                val jobDetail =
                    JobBuilder.newJob(PositionProductJob::class.java).withIdentity(jobKey).build()
                val triggerFactoryBean = SimpleTriggerFactoryBean().apply {
                    setName(jobIdentity)
                    setStartTime(Date())
                    setRepeatInterval(10000L)
                    setRepeatCount(0)
                    setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW)
                    afterPropertiesSet()
                }.getObject()
                jobDetail.jobDataMap[JOB_CATEGORY_ID] = categoryId
                try {
                    val schedulerFactoryBean = applicationContext.getBean(Scheduler::class.java)
                    val checkExists = schedulerFactoryBean.checkExists(jobKey)

                    if (checkExists) continue

                    var triggerKeys: MutableSet<TriggerKey> = schedulerFactoryBean.getTriggerKeys(
                        GroupMatcher.triggerGroupEquals("DEFAULT")
                    )
                    delay_loop@ while (triggerKeys.size >= 40 || schedulerFactoryBean.currentlyExecutingJobs.size >= 40) {
                        log.info { "Too match product position job in queue. Delay loop" }
                        delay(100_000)
                        triggerKeys = schedulerFactoryBean.getTriggerKeys(
                            GroupMatcher.triggerGroupEquals("DEFAULT")
                        )
                        continue@delay_loop
                    }
                    log.info { "Schedule product position job $jobIdentity" }
                    schedulerFactoryBean.scheduleJob(jobDetail, triggerFactoryBean)
                } catch (e: ObjectAlreadyExistsException) {
                    log.warn { "Task still in progress: $jobIdentity" }
                } catch (e: Exception) {
                    log.error(e) { "Failed to start position fetch scheduler job" }
                }
            }
        }
    }

    companion object {
        const val JOB_CATEGORY_ID = "categoryId"
    }
}
