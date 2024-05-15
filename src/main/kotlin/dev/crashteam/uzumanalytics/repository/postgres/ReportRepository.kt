package dev.crashteam.uzumanalytics.repository.postgres

import dev.crashteam.uzumanalytics.db.model.enums.ReportStatus
import dev.crashteam.uzumanalytics.domain.mongo.ReportType
import dev.crashteam.uzumanalytics.db.model.tables.records.ReportsRecord
import java.time.LocalDateTime

interface ReportRepository {

    fun findByJobId(jobId: String): ReportsRecord?

    fun findAllByStatus(reportStatus: ReportStatus): List<ReportsRecord>

    fun findByRequestIdAndSellerLink(requestId: String, sellerLink: String): ReportsRecord?

    fun findByRequestIdAndCategoryPublicId(requestId: String, categoryPublicId: Long): ReportsRecord?

    fun countByUserIdAndCreatedAtBetweenAndReportType(
        userId: String,
        fromTime: LocalDateTime,
        toTime: LocalDateTime,
        reportType: ReportType
    ): Long

}
