package dev.crashteam.uzumanalytics.repository.mongo.model

import dev.crashteam.uzumanalytics.mongo.ProductSkuData
import dev.crashteam.uzumanalytics.mongo.ProductSkuId

data class MultipleProductHistorySales(
    val id: ProductSkuId,
    val seller: MultipleProductHistorySalesSeller,
    val skuChange: List<ProductSkuData>
)

data class MultipleProductHistorySalesSeller(
    val title: String,
    val link: String,
    val accountId: Long?,
    val sellerAccountId: Long?
)

