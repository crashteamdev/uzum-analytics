package dev.crashteam.uzumanalytics.job

import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import dev.crashteam.uzumanalytics.domain.mongo.ReportStatus
import dev.crashteam.uzumanalytics.domain.mongo.ReportVersion
import dev.crashteam.uzumanalytics.extensions.getApplicationContext
import dev.crashteam.uzumanalytics.report.ReportFileService
import dev.crashteam.uzumanalytics.report.ReportService
import dev.crashteam.uzumanalytics.repository.mongo.ReportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import org.quartz.Job
import org.quartz.JobExecutionContext
import java.math.BigInteger
import java.nio.file.Files
import java.time.LocalDateTime
import java.util.*
import kotlin.io.path.deleteIfExists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

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
        val userId = context.jobDetail.jobDataMap["user_id"] as? String
        val version = ReportVersion.valueOf(context.jobDetail.jobDataMap["version"] as? String ?: ReportVersion.V1.name)
        val now = LocalDateTime.now().toLocalDate().atStartOfDay()
        val fromTime = now.minusDays(interval.toLong())
        val toTime = now
        runBlocking {
            val tempFilePath = withContext(Dispatchers.IO) {
                Files.createTempFile("link-${UUID.randomUUID()}", "")
            }
            try {
                log.info { "Generating report job. sellerLink=${sellerLink}; jobId=${jobId}; version=$version" }

                if (version == ReportVersion.V2) {
                    reportFileService.generateReportBySellerV2(
                        sellerLink,
                        fromTime,
                        toTime,
                        tempFilePath.outputStream()
                    )
                    log.info(
                        "Save generated seller file. name=${tempFilePath.fileName};" +
                                " size=${FileUtils.byteCountToDisplaySize(BigInteger.valueOf(tempFilePath.toFile().length()))}",
                    )
                    reportService.saveSellerReportV2(sellerLink, interval, jobId, tempFilePath.inputStream())
                } else if (version == ReportVersion.V1) {
                    val generatedReport = reportFileService.generateReportBySeller(sellerLink, fromTime, toTime)
                    reportService.saveSellerReport(sellerLink, interval, jobId, generatedReport)
                } else {
                    throw IllegalStateException("Unknown report version: $version")
                }
            } catch (e: Exception) {
                log.error(e) { "Failed to generate report. seller=$sellerLink; interval=$interval; jobId=$jobId" }
                val reportRepository = applicationContext.getBean(ReportRepository::class.java)
                reportRepository.updateReportStatus(jobId, ReportStatus.FAILED).awaitSingleOrNull()
            } finally {
                tempFilePath.deleteIfExists()
            }
        }
    }
}
