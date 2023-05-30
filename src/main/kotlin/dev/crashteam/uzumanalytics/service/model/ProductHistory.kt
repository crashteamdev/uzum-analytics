package dev.crashteam.uzumanalytics.service.model

import dev.crashteam.uzumanalytics.domain.mongo.ProductSkuData
import dev.crashteam.uzumanalytics.domain.mongo.ProductSkuId

data class ProductHistory(
    val id: ProductSkuId,
    val skuChange: List<ProductSkuData>
)
