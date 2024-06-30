package dev.crashteam.uzumanalytics.job

import dev.crashteam.uzumanalytics.extensions.getApplicationContext
import dev.crashteam.uzumanalytics.report.ReportFileService
import dev.crashteam.uzumanalytics.report.ReportService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
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
        val now = LocalDateTime.now().toLocalDate().atStartOfDay()
        val fromTime = now.minusDays(interval.toLong())
        val toTime = now
        runBlocking {
            val tempFilePath = withContext(Dispatchers.IO) {
                Files.createTempFile("link-${UUID.randomUUID()}", "")
            }
            try {
                log.info { "Generating report job. sellerLink=${sellerLink}; jobId=${jobId}" }
                reportFileService.generateReportBySellerV2(
                    sellerLink,
                    fromTime,
                    toTime,
                    tempFilePath.outputStream()
                )
                log.info(
                    "Save generated seller file. name=${tempFilePath.fileName};" +
                            " size=${
                                FileUtils.byteCountToDisplaySize(
                                    BigInteger.valueOf(
                                        tempFilePath.toFile().length()
                                    )
                                )
                            }",
                )
                reportService.saveSellerReportV2(sellerLink, interval, jobId, tempFilePath.inputStream())
            } catch (e: Exception) {
                log.error(e) { "Failed to generate report. seller=$sellerLink; interval=$interval; jobId=$jobId" }
                val reportRepository =
                    applicationContext.getBean(dev.crashteam.uzumanalytics.repository.postgres.ReportRepository::class.java)
                reportRepository.updateReportStatusByJobId(
                    jobId,
                    dev.crashteam.uzumanalytics.db.model.enums.ReportStatus.failed
                )
            } finally {
                tempFilePath.deleteIfExists()
            }
        }
    }
}
