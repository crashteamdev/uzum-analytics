package dev.crashteam.uzumanalytics.repository.mongo

import com.mongodb.bulk.BulkWriteResult
import dev.crashteam.uzumanalytics.domain.mongo.SellerDetailDocument
import reactor.core.publisher.Mono

interface SellerCustomRepository {
    fun saveSellerBatch(sellers: Collection<SellerDetailDocument>): Mono<BulkWriteResult>
}
