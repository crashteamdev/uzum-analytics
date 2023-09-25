package dev.crashteam.uzumanalytics.repository.mongo

import dev.crashteam.uzumanalytics.domain.mongo.PromoCodeDocument
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
interface PromoCodeRepository : ReactiveCrudRepository<PromoCodeDocument, String> {

    fun findByCode(code: String): Mono<PromoCodeDocument>
}
