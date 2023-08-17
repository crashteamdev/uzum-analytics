package dev.crashteam.uzumanalytics.repository.clickhouse.model

import java.math.BigDecimal

data class ChSellerOverallInfo(
    val averagePrice: BigDecimal,
    val revenue: BigDecimal,
    val orderCount: Long,
    val productCount: Long,
    val productCountWithSales: Long,
    val productCountWithoutSales: Long,
)
