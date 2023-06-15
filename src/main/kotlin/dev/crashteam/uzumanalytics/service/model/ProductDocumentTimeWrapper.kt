package dev.crashteam.uzumanalytics.service.model

import dev.crashteam.uzumanalytics.domain.mongo.ProductDocument

class ProductDocumentTimeWrapper(
    val productDocument: ProductDocument,
    val time: Long,
)
