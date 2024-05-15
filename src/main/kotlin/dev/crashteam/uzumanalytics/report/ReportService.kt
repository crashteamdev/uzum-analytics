package dev.crashteam.uzumanalytics.report

import dev.crashteam.uzumanalytics.db.model.enums.ReportStatus
import dev.crashteam.uzumanalytics.db.model.enums.ReportType
import dev.crashteam.uzumanalytics.repository.postgres.ReportRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalTime

@Service
class ReportService(
    private val reportFileService: ReportFileService,
    private val reportRepository: ReportRepository,
) {

    @Transactional
    suspend fun saveSellerReportV2(sellerLink: String, interval: Int, jobId: String, reportInputStream: InputStream) {
        val reportId: String = reportFileService.saveReport(jobId, reportInputStream)
        reportRepository.updateReportStatusByJobId(jobId, ReportStatus.completed)
        reportRepository.updateReportIdByJobId(jobId, reportId)
    }

    @Transactional
    suspend fun saveSellerReport(sellerLink: String, interval: Int, jobId: String, report: ByteArray) {
        val reportId: String =
            reportFileService.saveReport(jobId, report.inputStream())
        reportRepository.updateReportStatusByJobId(jobId, ReportStatus.completed)
        reportRepository.updateReportIdByJobId(jobId, reportId)
    }

    @Transactional
    suspend fun saveCategoryReportV2(
        categoryPublicId: Long,
        interval: Int,
        jobId: String,
        reportInputStream: InputStream
    ) {
        val reportId: String = reportFileService.saveReport(jobId, reportInputStream)
        reportRepository.updateReportStatusByJobId(jobId, ReportStatus.completed)
        reportRepository.updateReportIdByJobId(jobId, reportId)
    }

    @Transactional
    suspend fun saveCategoryReport(
        categoryPublicId: Long,
        categoryTitle: String,
        interval: Int,
        jobId: String,
        report: ByteArray
    ) {
        val reportId: String = reportFileService.saveReport(jobId, report.inputStream())
        reportRepository.updateReportStatusByJobId(jobId, ReportStatus.completed)
        reportRepository.updateReportIdByJobId(jobId, reportId)
    }

    suspend fun getUserShopReportDailyReportCount(userId: String): Int {
        return reportRepository.countByUserIdAndCreatedAtBetweenAndReportType(
            userId,
            LocalDate.now().atStartOfDay(),
            LocalDate.now().atTime(LocalTime.MAX),
            ReportType.seller
        )
    }

    fun getUserShopReportDailyReportCountV2(userId: String): Int {
        return reportRepository.countByUserIdAndCreatedAtBetweenAndReportType(
            userId,
            LocalDate.now().atStartOfDay(),
            LocalDate.now().atTime(LocalTime.MAX),
            ReportType.seller
        )
    }

    suspend fun getUserCategoryReportDailyReportCount(userId: String): Int {
        return reportRepository.countByUserIdAndCreatedAtBetweenAndReportType(
            userId,
            LocalDate.now().atStartOfDay(),
            LocalDate.now().atTime(LocalTime.MAX),
            ReportType.category
        )
    }

    fun getUserCategoryReportDailyReportCountV2(userId: String): Int {
        return reportRepository.countByUserIdAndCreatedAtBetweenAndReportType(
            userId,
            LocalDate.now().atStartOfDay(),
            LocalDate.now().atTime(LocalTime.MAX),
            ReportType.category
        )
    }

}
