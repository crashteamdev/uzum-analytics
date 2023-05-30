package dev.crashteam.uzumanalytics.repository.mongo

import dev.crashteam.uzumanalytics.repository.mongo.model.ProductPositionAggregate
import reactor.core.publisher.Flux
import java.time.LocalDateTime

interface ProductPositionCustomRepository {

    fun findProductPositions(
        categoryId: Long,
        productId: Long,
        skuId: Long,
        fromTime: LocalDateTime,
        toTime: LocalDateTime,
    ): Flux<ProductPositionAggregate>
}
