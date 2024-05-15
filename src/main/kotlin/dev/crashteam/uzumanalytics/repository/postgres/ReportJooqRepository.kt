package dev.crashteam.uzumanalytics.repository.postgres

import dev.crashteam.uzumanalytics.db.model.enums.ReportStatus
import dev.crashteam.uzumanalytics.db.model.tables.Reports.REPORTS
import dev.crashteam.uzumanalytics.db.model.tables.records.ReportsRecord
import dev.crashteam.uzumanalytics.domain.mongo.ReportType
import org.jooq.DSLContext
import org.jooq.kotlin.fetchList
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ReportJooqRepository(
    private val dsl: DSLContext
) : ReportRepository {

    override fun findByJobId(jobId: String): ReportsRecord? {
        val r = REPORTS
        return dsl.selectFrom(r)
            .where(r.JOB_ID.eq(jobId))
            .fetchOne()
    }

    override fun findAllByStatus(reportStatus: ReportStatus): List<ReportsRecord> {
        val r = REPORTS
        return dsl.select(r)
            .where(r.STATUS.eq(reportStatus))
            .fetchList()
    }

    override fun findByRequestIdAndSellerLink(requestId: String, sellerLink: String): ReportsRecord? {
        val r = REPORTS
        return dsl.selectFrom(r)
            .where(r.REPORT_ID.eq(requestId).and(r.SELLER_LINK.eq(sellerLink)))
            .fetchOne()
    }

    override fun findByRequestIdAndCategoryPublicId(requestId: String, categoryPublicId: Long): ReportsRecord? {
        val r = REPORTS
        return dsl.selectFrom(r)
            .where(r.REPORT_ID.eq(requestId).and(r.CATEGORYID.eq(categoryPublicId.toString())))
            .fetchOne()
    }

    override fun countByUserIdAndCreatedAtBetweenAndReportType(
        userId: String,
        fromTime: LocalDateTime,
        toTime: LocalDateTime,
        reportType: ReportType
    ): Long {
        TODO("Not yet implemented")
    }


}
