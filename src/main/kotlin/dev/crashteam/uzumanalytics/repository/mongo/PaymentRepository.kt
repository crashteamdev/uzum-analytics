package dev.crashteam.uzumanalytics.repository.mongo

import dev.crashteam.uzumanalytics.domain.mongo.PaymentDocument
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Repository
interface PaymentRepository : ReactiveCrudRepository<PaymentDocument, String> {

    fun findByStatus(status: String): Flux<PaymentDocument>

    fun findByStatusAndPaymentSystem(status: String, paymentSystem: String): Flux<PaymentDocument>

    fun findByPaymentId(paymentId: String): Mono<PaymentDocument>

    fun removeByPaymentId(paymentId: String): Mono<PaymentDocument>

    @Query("{'paycomDocument.paycomId': ?0}")
    fun findByPaycomId(paycomId: String): Mono<PaymentDocument>

    fun findByPaycomDocumentNotNullAndCreatedAtBetween(from: LocalDateTime, to: LocalDateTime): Flux<PaymentDocument>
}
