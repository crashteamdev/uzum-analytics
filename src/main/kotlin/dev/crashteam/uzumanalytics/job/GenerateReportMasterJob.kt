package dev.crashteam.uzumanalytics.job

import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import dev.crashteam.uzumanalytics.domain.mongo.ReportDocument
import dev.crashteam.uzumanalytics.domain.mongo.ReportStatus
import dev.crashteam.uzumanalytics.domain.mongo.ReportType
import dev.crashteam.uzumanalytics.extensions.getApplicationContext
import dev.crashteam.uzumanalytics.repository.mongo.ReportRepository
import org.quartz.*
import org.quartz.impl.matchers.GroupMatcher
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean
import java.util.*

private val log = KotlinLogging.logger {}

class GenerateReportMasterJob : Job {

    override fun execute(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val reportRepository = applicationContext.getBean(ReportRepository::class.java)
        runBlocking {
            val reportDocuments =
                reportRepository.findAllByStatus(ReportStatus.PROCESSING).collectList().awaitSingleOrNull()
            val schedulerFactoryBean = applicationContext.getBean(Scheduler::class.java)
            reportDocuments?.forEach { reportDoc ->
                when (reportDoc.reportType) {
                    ReportType.SELLER -> {
                        log.info { "Schedule job report. sellerLink=${reportDoc.sellerLink}; jobId=${reportDoc.jobId}" }
                        val jobIdentity = "${reportDoc.sellerLink}-seller-report-${reportDoc.jobId}-Job"
                        scheduleShopReportJob(jobIdentity, reportDoc, schedulerFactoryBean)
                    }
                    ReportType.CATEGORY -> {
                        log.info { "Schedule job report. categoryPath=${reportDoc.sellerLink}; jobId=${reportDoc.jobId}" }
                        val jobIdentity = "${reportDoc.categoryPublicId}-category-report-${reportDoc.jobId}-Job"
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

    private fun scheduleShopReportJob(jobIdentity: String, reportDoc: ReportDocument, schedulerFactoryBean: Scheduler) {
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
        if (!schedulerFactoryBean.checkExists(jobKey)) {
            schedulerFactoryBean.scheduleJob(jobDetail, triggerFactoryBean)
        }
    }

    private fun schedulerCategoryReportJob(
        jobIdentity: String,
        reportDoc: ReportDocument,
        schedulerFactoryBean: Scheduler
    ) {
        val triggerKeys: MutableSet<TriggerKey> = schedulerFactoryBean.getTriggerKeys(
            GroupMatcher.triggerGroupEquals("CATEGORY")
        )
        if (triggerKeys.size > 0) {
            return
        }
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
            setGroup("CATEGORY")
            afterPropertiesSet()
        }.getObject()
        jobDetail.jobDataMap["categoryPublicId"] = reportDoc.categoryPublicId
        jobDetail.jobDataMap["interval"] = reportDoc.interval
        jobDetail.jobDataMap["job_id"] = reportDoc.jobId
        if (!schedulerFactoryBean.checkExists(jobKey)) {
            schedulerFactoryBean.scheduleJob(jobDetail, triggerFactoryBean)
        }
    }
}
