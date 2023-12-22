package dev.crashteam.uzumanalytics.config

import dev.crashteam.uzumanalytics.config.properties.UzumProperties
import dev.crashteam.uzumanalytics.job.*
import dev.crashteam.uzumanalytics.stream.scheduler.PendingMessageScheduler
import org.quartz.*
import org.quartz.impl.JobDetailImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct

@Configuration
@ConditionalOnProperty(
    value = ["uzum.scheduleEnabled"],
    havingValue = "true",
    matchIfMissing = true
)
class JobConfiguration(
    private val uzumProperties: UzumProperties,
) {

    @Autowired
    private lateinit var schedulerFactoryBean: Scheduler

    @PostConstruct
    fun init() {
        schedulerFactoryBean.addJob(reportCleanupJob(), true, true)
        if (!schedulerFactoryBean.checkExists(TriggerKey(REPORT_CLEANUP_JOB, REPORT_CLEANUP_GROUP))) {
            schedulerFactoryBean.scheduleJob(triggerReportCleanupJob())
        }
        schedulerFactoryBean.addJob(reportGenerateMasterJob(), true, true)
        if (!schedulerFactoryBean.checkExists(TriggerKey(REPORT_GENERATE_MASTER_JOB, REPORT_GENERATE_MASTER_GROUP))) {
            schedulerFactoryBean.scheduleJob(triggerReportGenerateMasterJob())
        }
//        schedulerFactoryBean.addJob(pendingMessageJob(), true, true)
//        if (!schedulerFactoryBean.checkExists(TriggerKey(PENDING_MESSAGE_JOB, PENDING_MESSAGE_GROUP))) {
//            schedulerFactoryBean.scheduleJob(triggerPendingMessageJob())
//        }
    }

//
//    private fun paymentJob(): JobDetailImpl {
//        val jobDetail = JobDetailImpl()
//        jobDetail.key = JobKey(PAYMENT_JOB, PAYMENT_JOB_GROUP)
//        jobDetail.jobClass = PaymentMasterJob::class.java
//
//        return jobDetail
//    }
//
//    private fun triggerPaymentJob(): CronTrigger {
//        return TriggerBuilder.newTrigger()
//            .forJob(paymentJob())
//            .withIdentity(PAYMENT_JOB, PAYMENT_JOB_GROUP)
//            .withSchedule(CronScheduleBuilder.cronSchedule(uzumProperties.paymentCron))
//            .withPriority(Int.MAX_VALUE)
//            .build()
//    }

//    private fun pendingMessageJob(): JobDetailImpl {
//        val jobDetail = JobDetailImpl()
//        jobDetail.key = JobKey(PENDING_MESSAGE_JOB, PENDING_MESSAGE_GROUP)
//        jobDetail.jobClass = PendingMessageScheduler::class.java
//
//        return jobDetail
//    }
//
//    private fun triggerPendingMessageJob(): CronTrigger {
//        return TriggerBuilder.newTrigger()
//            .forJob(pendingMessageJob())
//            .withIdentity(PENDING_MESSAGE_JOB, PENDING_MESSAGE_GROUP)
//            .withSchedule(CronScheduleBuilder.cronSchedule(uzumProperties.pendingMessageCron))
//            .withPriority(Int.MAX_VALUE)
//            .build()
//    }

    private fun reportCleanupJob(): JobDetailImpl {
        val jobDetail = JobDetailImpl()
        jobDetail.key = JobKey(REPORT_CLEANUP_JOB, REPORT_CLEANUP_GROUP)
        jobDetail.jobClass = ReportCleanUpJob::class.java

        return jobDetail
    }

    private fun triggerReportCleanupJob(): CronTrigger {
        return TriggerBuilder.newTrigger()
            .forJob(reportCleanupJob())
            .withIdentity(REPORT_CLEANUP_JOB, REPORT_CLEANUP_GROUP)
            .withSchedule(CronScheduleBuilder.cronSchedule(uzumProperties.reportCleanUpCron))
            .build()
    }

    private fun reportGenerateMasterJob(): JobDetailImpl {
        val jobDetail = JobDetailImpl()
        jobDetail.key = JobKey(REPORT_GENERATE_MASTER_JOB, REPORT_GENERATE_MASTER_GROUP)
        jobDetail.jobClass = GenerateReportMasterJob::class.java

        return jobDetail
    }

    private fun triggerReportGenerateMasterJob(): CronTrigger {
        return TriggerBuilder.newTrigger()
            .forJob(reportGenerateMasterJob())
            .withIdentity(REPORT_GENERATE_MASTER_JOB, REPORT_GENERATE_MASTER_GROUP)
            .withSchedule(CronScheduleBuilder.cronSchedule(uzumProperties.reportGenerateCron))
            .withPriority(Int.MAX_VALUE)
            .build()
    }

    companion object {
        const val REPORT_CLEANUP_JOB = "reportCleanupJob"
        const val REPORT_CLEANUP_GROUP = "reportCleanupGroup"
        const val REPORT_GENERATE_MASTER_JOB = "reportGenerateMasterJob"
        const val REPORT_GENERATE_MASTER_GROUP = "reportGenerateMasterGroup"
//        const val PENDING_MESSAGE_JOB = "pendingMessageJob"
//        const val PENDING_MESSAGE_GROUP = "pendingMessageGroup"
    }
}
