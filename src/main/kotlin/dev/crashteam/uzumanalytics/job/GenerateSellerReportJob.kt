package dev.crashteam.uzumanalytics.job

import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import dev.crashteam.uzumanalytics.domain.mongo.ReportStatus
import dev.crashteam.uzumanalytics.extensions.getApplicationContext
import dev.crashteam.uzumanalytics.report.ReportFileService
import dev.crashteam.uzumanalytics.report.ReportService
import dev.crashteam.uzumanalytics.repository.mongo.ReportRepository
import org.quartz.Job
import org.quartz.JobExecutionContext
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

class GenerateSellerReportJob : Job {

    override fun execute(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val reportFileService = applicationContext.getBean(ReportFileService::class.java)
        val reportService = applicationContext.getBean(ReportService::class.java)
        val sellerLink = context.jobDetail.jobDataMap["sellerLink"] as? String
            ?: throw IllegalStateException("sellerLink can't be null")
        val interval = context.jobDetail.jobDataMap["interval"] as? Int
            ?: throw IllegalStateException("interval can't be null")
        val jobId = context.jobDetail.jobDataMap["job_id"] as? String
            ?: throw IllegalStateException("job_id can't be null")
        val now = LocalDateTime.now().toLocalDate().atStartOfDay()
        val fromTime = now.minusDays(interval.toLong())
        val toTime = now
        runBlocking {
            try {
                log.info { "Generating report job. sellerLink=${sellerLink}; jobId=${jobId}" }
                val generatedReport = reportFileService.generateReportBySeller(sellerLink, fromTime, toTime)
                reportService.saveSellerReport(sellerLink, jobId, generatedReport)
            } catch (e: Exception) {
                log.error(e) { "Failed to generate report. seller=$sellerLink; interval=$interval; jobId=$jobId" }
                val reportRepository = applicationContext.getBean(ReportRepository::class.java)
                reportRepository.updateReportStatus(jobId, ReportStatus.FAILED).awaitSingleOrNull()
            }
        }
    }
}
