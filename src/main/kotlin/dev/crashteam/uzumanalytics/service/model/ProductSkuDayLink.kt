package dev.crashteam.uzumanalytics.service.model

import dev.crashteam.uzumanalytics.repository.mongo.model.ProductHistorySkuChange

data class ProductSkuDayLink(
    val sku: ProductHistorySkuChange,
    val nextDaySku: ProductHistorySkuChange,
)
