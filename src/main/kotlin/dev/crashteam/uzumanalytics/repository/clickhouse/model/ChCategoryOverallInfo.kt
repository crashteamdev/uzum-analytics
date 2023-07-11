package dev.crashteam.uzumanalytics.repository.clickhouse.model

import java.math.BigDecimal

data class ChCategoryOverallInfo(
    val averagePrice: BigDecimal,
    val orderCount: Long,
    val sellerCount: Long,
    val salesPerSeller: BigDecimal,
    val productZeroSalesCount: Long,
    val sellersZeroSalesCount: Long,
)
