package dev.crashteam.uzumanalytics.repository.clickhouse.model

data class ChCategoryProductOrderChart(
    val productId: String,
    val orderChart: List<Long>,
)
