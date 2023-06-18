package dev.crashteam.uzumanalytics.repository.mongo

import dev.crashteam.uzumanalytics.domain.mongo.SellerDetailDocument
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface SellerRepository : ReactiveCrudRepository<SellerDetailDocument, String>, SellerCustomRepository {

    fun findByAccountId(accountId: Long): Flux<SellerDetailDocument>

    fun findByLink(sellerLink: String): Mono<SellerDetailDocument>
}
