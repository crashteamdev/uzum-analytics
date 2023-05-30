package dev.crashteam.uzumanalytics.repository.mongo.pageable

import dev.crashteam.uzumanalytics.domain.mongo.ProductSkuData

data class ProductHistoryPageResult(
    val productId: Long,
    val skuId: Long,
    val result: PageResult<ProductSkuData>?
)

data class ProductHistoryResultAggr(
    val skuChange: List<ProductSkuData>,
    val count: Int
)
