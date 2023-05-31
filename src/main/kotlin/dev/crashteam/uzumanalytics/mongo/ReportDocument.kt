package dev.crashteam.uzumanalytics.mongo

import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("reports")
data class ReportDocument(
    @Indexed
    val reportId: String?,
    @Indexed(unique = true)
    val requestId: String?,
    @Indexed(unique = true)
    val jobId: String,
    val userId: String,
    val period: Period? = null, // Deprecated
    val interval: Int?,
    val createdAt: LocalDateTime,
    val sellerLink: String? = null,
    val categoryPublicId: Long? = null,
    val reportType: ReportType? = null,
    val status: ReportStatus
)

enum class Period {
    THIRTY
}

enum class ReportType {
    SELLER, CATEGORY
}

enum class ReportStatus {
    PROCESSING, COMPLETED, FAILED, DELETED
}
