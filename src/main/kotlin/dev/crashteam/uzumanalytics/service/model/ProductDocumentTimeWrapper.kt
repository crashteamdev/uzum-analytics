package dev.crashteam.uzumanalytics.service.model

import dev.crashteam.uzumanalytics.domain.mongo.ProductDocument
import java.time.LocalDateTime

class ProductDocumentTimeWrapper(
    val productDocument: ProductDocument,
    val scrapTime: LocalDateTime,
    @Deprecated(message = "use scrapTime instead") val time: Long? = null,
)
