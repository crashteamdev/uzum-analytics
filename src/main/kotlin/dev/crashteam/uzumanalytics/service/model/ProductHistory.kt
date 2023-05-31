package dev.crashteam.uzumanalytics.service.model

import dev.crashteam.uzumanalytics.mongo.ProductSkuData
import dev.crashteam.uzumanalytics.mongo.ProductSkuId

data class ProductHistory(
    val id: ProductSkuId,
    val skuChange: List<ProductSkuData>
)
