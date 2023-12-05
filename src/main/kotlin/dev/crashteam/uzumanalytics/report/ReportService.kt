package dev.crashteam.uzumanalytics.report

import dev.crashteam.uzumanalytics.domain.mongo.ReportStatus
import dev.crashteam.uzumanalytics.domain.mongo.ReportType
import dev.crashteam.uzumanalytics.repository.mongo.ReportRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
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
        val reportId: String =
            reportFileService.saveSellerReport(sellerLink, jobId, reportInputStream, "$sellerLink-$interval.xlsx")
        reportRepository.updateReportStatus(jobId, ReportStatus.COMPLETED).awaitSingleOrNull()
        reportRepository.setReportId(jobId, reportId).awaitSingleOrNull()
    }

    @Transactional
    suspend fun saveSellerReport(sellerLink: String, interval: Int, jobId: String, report: ByteArray) {
        val reportId: String =
            reportFileService.saveSellerReport(sellerLink, jobId, report.inputStream(), "$sellerLink-$interval.xlsx")
        reportRepository.updateReportStatus(jobId, ReportStatus.COMPLETED).awaitSingleOrNull()
        reportRepository.setReportId(jobId, reportId).awaitSingleOrNull()
    }

    @Transactional
    suspend fun saveCategoryReportV2(
        categoryPublicId: Long,
        interval: Int,
        jobId: String,
        reportInputStream: InputStream
    ) {
        val reportId: String = reportFileService.saveCategoryReport(
            categoryPublicId,
            jobId,
            reportInputStream,
            "$categoryPublicId-$interval.xlsx"
        )
        reportRepository.updateReportStatus(jobId, ReportStatus.COMPLETED).awaitSingleOrNull()
        reportRepository.setReportId(jobId, reportId).awaitSingleOrNull()
    }

    @Transactional
    suspend fun saveCategoryReport(
        categoryPublicId: Long,
        categoryTitle: String,
        interval: Int,
        jobId: String,
        report: ByteArray
    ) {
        val reportId: String = reportFileService.saveCategoryReport(
            categoryPublicId,
            jobId,
            report.inputStream(),
            "$categoryTitle-$interval.xlsx"
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

    fun getUserShopReportDailyReportCountV2(userId: String): Mono<Long> {
        return reportRepository.countByUserIdAndCreatedAtBetweenAndReportType(
            userId,
            LocalDate.now().atStartOfDay(),
            LocalDate.now().atTime(LocalTime.MAX),
            ReportType.SELLER,
        )
    }

    suspend fun getUserCategoryReportDailyReportCount(userId: String): Long? {
        return reportRepository.countByUserIdAndCreatedAtBetweenAndReportType(
            userId,
            LocalDate.now().atStartOfDay(),
            LocalDate.now().atTime(LocalTime.MAX),
            ReportType.CATEGORY,
        ).awaitSingleOrNull()
    }

    fun getUserCategoryReportDailyReportCountV2(userId: String): Mono<Long> {
        return reportRepository.countByUserIdAndCreatedAtBetweenAndReportType(
            userId,
            LocalDate.now().atStartOfDay(),
            LocalDate.now().atTime(LocalTime.MAX),
            ReportType.CATEGORY
        )
    }

}
