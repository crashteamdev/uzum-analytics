package dev.crashteam.uzumanalytics.repository.clickhouse.model

import java.time.LocalDate

data class ChSellerOrderDynamic(
    val date: LocalDate,
    val orderAmount: Long,
)
