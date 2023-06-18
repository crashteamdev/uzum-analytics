package dev.crashteam.uzumanalytics.job

import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import dev.crashteam.uzumanalytics.domain.mongo.ReportStatus
import dev.crashteam.uzumanalytics.extensions.getApplicationContext
import dev.crashteam.uzumanalytics.report.ReportFileService
import dev.crashteam.uzumanalytics.repository.mongo.ReportRepository
import org.quartz.Job
import org.quartz.JobExecutionContext
import java.time.LocalDateTime

class ReportCleanUpJob : Job {

    override fun execute(context: JobExecutionContext) {
        val appContext = context.getApplicationContext()
        val reportFileService = appContext.getBean(ReportFileService::class.java)
        val reportRepository = appContext.getBean(ReportRepository::class.java)
        runBlocking {
            val minusHours = LocalDateTime.now().minusHours(18)
            reportFileService.findReportWithTtl(minusHours).forEach {
                val jobId = it.metadata?.get("job_id") as? String
                if (jobId != null) {
                    reportRepository.updateReportStatus(jobId, ReportStatus.DELETED).awaitSingleOrNull()
                }
            }
            reportFileService.deleteReportWithTtl(minusHours)
        }
    }
}
