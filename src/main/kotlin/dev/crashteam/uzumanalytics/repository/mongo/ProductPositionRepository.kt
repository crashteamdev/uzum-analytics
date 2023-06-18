package dev.crashteam.uzumanalytics.repository.mongo

import dev.crashteam.uzumanalytics.domain.mongo.ProductPositionTSDocument
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductPositionRepository : ReactiveCrudRepository<ProductPositionTSDocument, String>,
    ProductPositionCustomRepository
