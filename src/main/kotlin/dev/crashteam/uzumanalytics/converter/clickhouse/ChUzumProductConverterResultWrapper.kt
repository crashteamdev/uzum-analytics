package dev.crashteam.uzumanalytics.converter.clickhouse

import dev.crashteam.uzumanalytics.repository.clickhouse.model.ChUzumProduct

data class ChUzumProductConverterResultWrapper(
    val result: List<ChUzumProduct>
)
