package dev.crashteam.uzumanalytics.report

import dev.crashteam.uzumanalytics.domain.mongo.ReportStatus
import dev.crashteam.uzumanalytics.domain.mongo.ReportType
import dev.crashteam.uzumanalytics.repository.mongo.ReportRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime

@Service
class ReportService(
    private val reportFileService: ReportFileService,
    private val reportRepository: ReportRepository,
) {

    @Transactional
    suspend fun saveSellerReport(sellerLink: String, jobId: String, report: ByteArray) {
        val reportId: String =
            reportFileService.saveSellerReport(sellerLink, jobId, report.inputStream(), "$sellerLink.xlsx")
        reportRepository.updateReportStatus(jobId, ReportStatus.COMPLETED).awaitSingleOrNull()
        reportRepository.setReportId(jobId, reportId).awaitSingleOrNull()
    }

    @Transactional
    suspend fun saveCategoryReport(categoryPublicId: Long, categoryTitle: String, jobId: String, report: ByteArray) {
        val reportId: String = reportFileService.saveCategoryReport(
            categoryPublicId,
            categoryTitle,
            jobId,
            report.inputStream(),
            "$categoryTitle.xlsx"
        )
        reportRepository.updateReportStatus(jobId, ReportStatus.COMPLETED).awaitSingleOrNull()
        reportRepository.setReportId(jobId, reportId).awaitSingleOrNull()
    }

    suspend fun getUserShopReportDailyReportCount(userId: String): Long? {
        return reportRepository.countByUserIdAndCreatedAtBetweenAndReportType(
            userId,
            LocalDate.now().atStartOfDay(),
            LocalDate.now().atTime(LocalTime.MAX),
            ReportType.SELLER,
        ).awaitSingleOrNull()
    }

    suspend fun getUserCategoryReportDailyReportCount(userId: String): Long? {
        return reportRepository.countByUserIdAndCreatedAtBetweenAndReportType(
            userId,
            LocalDate.now().atStartOfDay(),
            LocalDate.now().atTime(LocalTime.MAX),
            ReportType.CATEGORY
        ).awaitSingleOrNull()
    }

}
