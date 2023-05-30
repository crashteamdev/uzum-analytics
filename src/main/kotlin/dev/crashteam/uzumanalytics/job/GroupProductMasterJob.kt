package dev.crashteam.uzumanalytics.job

import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import dev.crashteam.uzumanalytics.extensions.getApplicationContext
import dev.crashteam.uzumanalytics.repository.mongo.CategoryRepository
import org.quartz.*
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

private val log = KotlinLogging.logger {}

@DisallowConcurrentExecution
class GroupProductMasterJob : Job {

    private val jobPriority = AtomicInteger(Integer.MAX_VALUE)

    private var priorityStage = PRIORITY_STAGE.DOWN

    override fun execute(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val categoryRepository = applicationContext.getBean(CategoryRepository::class.java)
        if (priorityStage == PRIORITY_STAGE.DOWN) {
            jobPriority.set(Integer.MAX_VALUE)
        } else {
            jobPriority.set(Integer.MIN_VALUE)
        }
        runBlocking {
            val categories = categoryRepository.findByPathIsNull().collectList().awaitSingleOrNull()
            categories?.forEach { categoryDocument ->
                val jobIdentity = "${categoryDocument.publicId}-category-Job"
                val jobDetail =
                    JobBuilder.newJob(GroupProductCollectorJob::class.java).withIdentity(jobIdentity).build()
                val triggerFactoryBean = SimpleTriggerFactoryBean().apply {
                    setName(jobIdentity)
                    setStartTime(Date())
                    setRepeatInterval(0L)
                    setRepeatCount(0)
                    setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW)
                    setPriority(jobPriority.get())
                    afterPropertiesSet()
                }.getObject()
                jobDetail.jobDataMap["categoryId"] = categoryDocument.publicId
                try {
                    val schedulerFactoryBean = applicationContext.getBean(Scheduler::class.java)
                    schedulerFactoryBean.scheduleJob(jobDetail, triggerFactoryBean)
                } catch (e: ObjectAlreadyExistsException) {
                    log.warn { "Task still in progress: $jobIdentity" }
                } catch (e: Exception) {
                    log.error(e) { "Failed to start scheduler job" }
                } finally {
                    if (priorityStage == PRIORITY_STAGE.DOWN) {
                        jobPriority.decrementAndGet()
                    } else {
                        jobPriority.incrementAndGet()
                    }
                }
            }
            if (categories?.isNotEmpty() == true) {
                priorityStage = if (priorityStage == PRIORITY_STAGE.DOWN) PRIORITY_STAGE.UP else PRIORITY_STAGE.DOWN
            }
        }
    }

    private enum class PRIORITY_STAGE {
        UP, DOWN
    }
}
