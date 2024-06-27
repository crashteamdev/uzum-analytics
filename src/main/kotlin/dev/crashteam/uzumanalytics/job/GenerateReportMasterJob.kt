package dev.crashteam.uzumanalytics.job

import dev.crashteam.uzumanalytics.db.model.tables.pojos.Reports
import dev.crashteam.uzumanalytics.extensions.getApplicationContext
import dev.crashteam.uzumanalytics.repository.postgres.ReportRepository
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.quartz.*
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean
import java.util.*

private val log = KotlinLogging.logger {}

class GenerateReportMasterJob : Job {

    override fun execute(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val reportRepository = applicationContext.getBean(ReportRepository::class.java)
        runBlocking {
            val reportDocuments =
                reportRepository.findAllByStatus(dev.crashteam.uzumanalytics.db.model.enums.ReportStatus.processing)
            val schedulerFactoryBean = applicationContext.getBean(Scheduler::class.java)
            reportDocuments.forEach { reportDoc ->
                when (reportDoc.reportType) {
                    dev.crashteam.uzumanalytics.db.model.enums.ReportType.seller -> {
                        log.info { "Schedule job report. sellerLink=${reportDoc.sellerLink}; jobId=${reportDoc.jobId}" }
                        val jobIdentity = "${reportDoc.sellerLink}-seller-report-${reportDoc.jobId}-Job"
                        scheduleShopReportJob(jobIdentity, reportDoc, schedulerFactoryBean)
                    }

                    dev.crashteam.uzumanalytics.db.model.enums.ReportType.category -> {
                        log.info { "Schedule job report. categoryId=${reportDoc.categoryId}; jobId=${reportDoc.jobId}" }
                        val jobIdentity = "${reportDoc.categoryId}-category-report-${reportDoc.jobId}-Job"
                        schedulerCategoryReportJob(jobIdentity, reportDoc, schedulerFactoryBean)
                    }

                    else -> {
                        if (reportDoc.sellerLink != null) {
                            // Execute shop report
                            val jobIdentity = "${reportDoc.sellerLink}-seller-report-${reportDoc.jobId}-Job"
                            scheduleShopReportJob(jobIdentity, reportDoc, schedulerFactoryBean)
                        }
                    }
                }
            }
        }
    }

    private fun scheduleShopReportJob(jobIdentity: String, reportDoc: Reports, schedulerFactoryBean: Scheduler) {
        log.info { "Schedule shop report job: $reportDoc" }
        val jobKey = JobKey(jobIdentity)
        val jobDetail =
            JobBuilder.newJob(GenerateSellerReportJob::class.java).withIdentity(jobKey).build()
        val triggerFactoryBean = SimpleTriggerFactoryBean().apply {
            setName(jobIdentity)
            setStartTime(Date())
            setRepeatInterval(30L)
            setRepeatCount(0)
            setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW)
            setPriority(Int.MAX_VALUE)
            afterPropertiesSet()
        }.getObject()
        jobDetail.jobDataMap["sellerLink"] = reportDoc.sellerLink
        jobDetail.jobDataMap["interval"] = reportDoc.interval
        jobDetail.jobDataMap["job_id"] = reportDoc.jobId
        jobDetail.jobDataMap["user_id"] = reportDoc.userId
        if (!schedulerFactoryBean.checkExists(jobKey)) {
            schedulerFactoryBean.scheduleJob(jobDetail, triggerFactoryBean)
        }
    }

    private fun schedulerCategoryReportJob(
        jobIdentity: String,
        reportDoc: Reports,
        schedulerFactoryBean: Scheduler
    ) {
        log.info { "Schedule category report job: $reportDoc" }
        val jobKey = JobKey(jobIdentity)
        val jobDetail =
            JobBuilder.newJob(GenerateCategoryReportJob::class.java).withIdentity(jobKey).build()
        val triggerFactoryBean = SimpleTriggerFactoryBean().apply {
            setName(jobIdentity)
            setStartTime(Date())
            setRepeatInterval(30L)
            setRepeatCount(0)
            setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW)
            setPriority(Int.MAX_VALUE)
            afterPropertiesSet()
        }.getObject()
        jobDetail.jobDataMap["categoryPublicId"] = reportDoc.categoryId
        jobDetail.jobDataMap["interval"] = reportDoc.interval
        jobDetail.jobDataMap["job_id"] = reportDoc.jobId
        jobDetail.jobDataMap["user_id"] = reportDoc.userId
        if (!schedulerFactoryBean.checkExists(jobKey)) {
            schedulerFactoryBean.scheduleJob(jobDetail, triggerFactoryBean)
        }
    }
}
