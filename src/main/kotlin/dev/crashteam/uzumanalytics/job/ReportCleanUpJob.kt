package dev.crashteam.uzumanalytics.job

import dev.crashteam.uzumanalytics.extensions.getApplicationContext
import dev.crashteam.uzumanalytics.report.ReportFileService
import kotlinx.coroutines.runBlocking
import org.quartz.Job
import org.quartz.JobExecutionContext
import java.time.LocalDateTime

class ReportCleanUpJob : Job {

    override fun execute(context: JobExecutionContext) {
        val appContext = context.getApplicationContext()
        val reportFileService = appContext.getBean(ReportFileService::class.java)
        runBlocking {
            val minusHours = LocalDateTime.now().minusHours(18)
            reportFileService.removeFileOlderThan(minusHours)
        }
    }
}
