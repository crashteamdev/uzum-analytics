package dev.crashteam.uzumanalytics.job

import dev.crashteam.uzumanalytics.extensions.getApplicationContext
import dev.crashteam.uzumanalytics.report.ReportFileService
import dev.crashteam.uzumanalytics.report.ReportService
import dev.crashteam.uzumanalytics.repository.postgres.ReportRepository
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

class GenerateCategoryReportJob : Job {

    override fun execute(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val reportFileService = applicationContext.getBean(ReportFileService::class.java)
        val reportService = applicationContext.getBean(ReportService::class.java)
        val categoryPublicId = context.jobDetail.jobDataMap["categoryPublicId"] as? Long
            ?: throw IllegalStateException("categoryPublicId can't be null")
        val categoryPath = context.jobDetail.jobDataMap["categoryPath"] as? String
        val interval = context.jobDetail.jobDataMap["interval"] as? Int
            ?: throw IllegalStateException("interval can't be null")
        val jobId = context.jobDetail.jobDataMap["job_id"] as? String
            ?: throw IllegalStateException("job_id can't be null")
        val now = LocalDateTime.now().toLocalDate().atStartOfDay()
        val fromTime = now.minusDays(interval.toLong())
        val toTime = now
        runBlocking {
            val tempFilePath = withContext(Dispatchers.IO) {
                Files.createTempFile("link-${UUID.randomUUID()}", "")
            }
            try {
                log.info { "Generating report job. categoryPublicId=$categoryPublicId; categoryPath=$categoryPath; jobId=$jobId;" }
                reportFileService.generateReportByCategoryV2(
                    categoryPublicId,
                    fromTime,
                    toTime,
                    tempFilePath.outputStream()
                )
                log.info(
                    "Save generated category file. name=${tempFilePath.fileName};" +
                            " size=${
                                FileUtils.byteCountToDisplaySize(
                                    BigInteger.valueOf(
                                        tempFilePath.toFile().length()
                                    )
                                )
                            }",
                )
                reportService.saveCategoryReportV2(
                    categoryPublicId,
                    interval,
                    jobId,
                    tempFilePath.inputStream()
                )
            } catch (e: Exception) {
                log.error(e) { "Failed to generate report. categoryPublicId=$categoryPublicId; interval=$interval; jobId=$jobId" }
                val reportRepository =
                    applicationContext.getBean(ReportRepository::class.java)
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
