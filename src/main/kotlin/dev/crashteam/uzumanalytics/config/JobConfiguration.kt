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
        schedulerFactoryBean.addJob(aggregateStatsJob(), true, true)
        if (!schedulerFactoryBean.checkExists(TriggerKey(AGGREGATE_STATS_JOB, AGGREGATE_STATS_JOB_GROUP))) {
            schedulerFactoryBean.scheduleJob(triggerAggregateStatsJob())
        }
    }

    private fun aggregateStatsJob(): JobDetailImpl {
        val jobDetail = JobDetailImpl()
        jobDetail.key = JobKey(AGGREGATE_STATS_JOB, AGGREGATE_STATS_JOB_GROUP)
        jobDetail.jobClass = AggregateStatsJob::class.java

        return jobDetail
    }

    private fun triggerAggregateStatsJob(): CronTrigger {
        return TriggerBuilder.newTrigger()
            .forJob(aggregateStatsJob())
            .withIdentity(AGGREGATE_STATS_JOB, AGGREGATE_STATS_JOB_GROUP)
            .withSchedule(CronScheduleBuilder.cronSchedule(uzumProperties.aggregateCron))
            .withPriority(Int.MAX_VALUE)
            .build()
    }

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
        const val AGGREGATE_STATS_JOB = "aggregateStatsJob"
        const val AGGREGATE_STATS_JOB_GROUP = "aggregateStatsJobGroup"
    }
}
