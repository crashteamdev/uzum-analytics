package dev.crashteam.uzumanalytics.repository.mongo

import com.mongodb.client.result.UpdateResult
import dev.crashteam.uzumanalytics.domain.mongo.ReportDocument
import dev.crashteam.uzumanalytics.domain.mongo.ReportStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

interface ReportCustomRepository {

    fun updateReportStatus(jobId: String, reportStatus: ReportStatus): Mono<UpdateResult>

    fun setReportId(jobId: String, reportId: String): Mono<UpdateResult>

    fun findTodayReportBySellerLink(sellerLink: String, userId: String): Flux<ReportDocument>

    fun findByUserIdAndCreatedAtFromTime(
        userId: String,
        fromTime: LocalDateTime,
    ): Flux<ReportDocument>

}
