package dev.crashteam.uzumanalytics.report

import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingleOrNull
import dev.crashteam.uzumanalytics.mongo.ReportStatus
import dev.crashteam.uzumanalytics.repository.mongo.ReportRepository
import org.springframework.data.redis.core.ReactiveRedisCallback
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.nio.ByteBuffer
import java.time.Duration


@Service
class ReportService(
    private val reportFileService: ReportFileService,
    private val reportRepository: ReportRepository,
    private val redisTemplate: ReactiveRedisTemplate<String, Long>,
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

    suspend fun incrementShopUserReportCount(userid: String) {
        val key = "${SHOP_REPORT_PREFIX}-$userid"
        incrementUserReportCount(key)
    }

    suspend fun getUserShopReportDailyReportCount(userId: String): Long? {
        val key = "${SHOP_REPORT_PREFIX}-$userId"
        return redisTemplate.opsForValue().get(key).awaitSingleOrNull()
    }

    suspend fun incrementCategoryUserReportCount(userid: String) {
        val key = "${CATEGORY_REPORT_PREFIX}-$userid"
        incrementUserReportCount(key)
    }

    suspend fun getUserCategoryReportDailyReportCount(userId: String): Long? {
        val key = "${CATEGORY_REPORT_PREFIX}-$userId"
        return redisTemplate.opsForValue().get(key).awaitSingleOrNull()
    }

    private suspend fun incrementUserReportCount(key: String): List<Any>? {
        return redisTemplate.execute(ReactiveRedisCallback<List<Any>> { connection ->
            val bbKey = ByteBuffer.wrap(key.toByteArray())
            Mono.zip(
                connection.numberCommands().incr(bbKey),
                connection.keyCommands().expire(bbKey, Duration.ofHours(24))
            ).then(Mono.empty())
        }).awaitFirstOrNull()
    }

    private companion object {
        const val SHOP_REPORT_PREFIX = "shop-report"
        const val CATEGORY_REPORT_PREFIX = "category-report"
    }

}
