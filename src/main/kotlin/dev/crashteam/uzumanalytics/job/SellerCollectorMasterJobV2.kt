package dev.crashteam.uzumanalytics.job

import kotlinx.coroutines.delay
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import dev.crashteam.uzumanalytics.extensions.getApplicationContext
import dev.crashteam.uzumanalytics.repository.mongo.ProductRepository
import org.quartz.*
import org.quartz.impl.matchers.GroupMatcher
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

private val log = KotlinLogging.logger {}

@DisallowConcurrentExecution
class SellerCollectorMasterJobV2 : Job {

    override fun execute(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val productRepository = applicationContext.getBean(ProductRepository::class.java)
        runBlocking {
            val sellerIds = productRepository.findDistinctSellerIds().collectList().awaitSingleOrNull() ?: emptyList()
            val batchFire = AtomicInteger(50)
            for (sellerId in sellerIds) {
                val jobIdentity = "$sellerId-seller-Job"
                val jobKey = JobKey(jobIdentity)
                val jobDetail =
                    JobBuilder.newJob(SellerCollectorJob::class.java).withIdentity(jobKey).build()
                val triggerFactoryBean = SimpleTriggerFactoryBean().apply {
                    setName(jobIdentity)
                    setStartTime(Date())
                    setRepeatInterval(0L)
                    setRepeatCount(0)
                    setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW)
                    afterPropertiesSet()
                }.getObject()
                jobDetail.jobDataMap["sellerId"] = sellerId
                try {
                    val schedulerFactoryBean = applicationContext.getBean(Scheduler::class.java)
                    val checkExists = schedulerFactoryBean.checkExists(jobKey)

                    if (checkExists) continue

                    var triggerKeys: MutableSet<TriggerKey> = schedulerFactoryBean.getTriggerKeys(
                        GroupMatcher.triggerGroupEquals("DEFAULT")
                    )
                    log.info { "Currently executing jobs for sellers: triggers=${triggerKeys.size}; cej=${schedulerFactoryBean.currentlyExecutingJobs.size}" }
                    while (triggerKeys.size >= 80 || schedulerFactoryBean.currentlyExecutingJobs.size >= 80) {
                        delay(100_000)
                        triggerKeys = schedulerFactoryBean.getTriggerKeys(
                            GroupMatcher.triggerGroupEquals("DEFAULT")
                        )
                        continue
                    }

                    log.info { "Schedule job $jobIdentity" }

                    // Do not fire all at once
                    if (batchFire.getAndDecrement() <= 0) {
                        delay(320_000)
                        batchFire.set(50)
                    }

                    schedulerFactoryBean.scheduleJob(jobDetail, triggerFactoryBean)
                } catch (e: ObjectAlreadyExistsException) {
                    log.warn { "Task still in progress: $jobIdentity" }
                } catch (e: Exception) {
                    log.error(e) { "Failed to start scheduler job" }
                }
            }
        }
    }
}
