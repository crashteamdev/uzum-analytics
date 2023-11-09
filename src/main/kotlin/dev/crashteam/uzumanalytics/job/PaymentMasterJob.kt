package dev.crashteam.uzumanalytics.job

import dev.crashteam.uzumanalytics.controller.model.PaymentProvider
import dev.crashteam.uzumanalytics.domain.mongo.PaymentDocument
import dev.crashteam.uzumanalytics.extensions.getApplicationContext
import dev.crashteam.uzumanalytics.repository.mongo.PaymentRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.quartz.*
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean
import java.util.*

private val log = KotlinLogging.logger {}

@DisallowConcurrentExecution
class PaymentMasterJob : Job {

    private var applicationContext: ApplicationContext? = null

    override fun execute(context: JobExecutionContext) {
        applicationContext = context.getApplicationContext()
        val paymentRepository = applicationContext!!.getBean(PaymentRepository::class.java)
        runBlocking {
            val payments = paymentRepository.findByStatusAndPaymentSystem("pending", PaymentProvider.UZUM_BANK.name)
            val paymentList = payments.collectList().awaitSingleOrNull() ?: emptyList()
            for (paymentDocument in paymentList) {
                val jobIdentity = "${paymentDocument.paymentId}-payment-Job"
                val isPaymentJobScheduled = scheduleUzumPaymentJob(jobIdentity, paymentDocument)
                if (!isPaymentJobScheduled) {
                    continue
                }
            }
        }
    }

    private fun scheduleUzumPaymentJob(jobIdentity: String, paymentDocument: PaymentDocument): Boolean {
        val jobKey = JobKey(jobIdentity)
        val jobDetail =
            JobBuilder.newJob(UzumPaymentJob::class.java).withIdentity(jobKey).build()
        val triggerFactoryBean = SimpleTriggerFactoryBean().apply {
            setName(jobIdentity)
            setStartTime(Date())
            setRepeatInterval(30L)
            setRepeatCount(0)
            setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW)
            setPriority(Int.MAX_VALUE)
            afterPropertiesSet()
        }.getObject()!!
        jobDetail.jobDataMap["paymentId"] = paymentDocument.paymentId
        jobDetail.jobDataMap["userId"] = paymentDocument.userId
        jobDetail.jobDataMap["subscriptionType"] = paymentDocument.subscriptionType
        jobDetail.jobDataMap["externalId"] = paymentDocument.externalId

        return schedulerJob(jobKey, jobDetail, triggerFactoryBean)
    }

    private fun schedulerJob(jobKey: JobKey, jobDetail: JobDetail, trigger: SimpleTrigger): Boolean {
        val schedulerFactoryBean = applicationContext!!.getBean(Scheduler::class.java)
        val checkExists = schedulerFactoryBean.checkExists(jobKey)

        if (checkExists) {
            return false
        }

        return try {
            schedulerFactoryBean.scheduleJob(jobDetail, trigger)
            true
        } catch (e: ObjectAlreadyExistsException) {
            log.warn { "Task still in progress: $jobKey" }
            false
        } catch (e: Exception) {
            log.error(e) { "Failed to start scheduler job" }
            false
        }
    }
}
