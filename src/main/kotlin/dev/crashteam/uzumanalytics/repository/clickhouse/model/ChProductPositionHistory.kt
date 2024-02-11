package dev.crashteam.uzumanalytics.repository.clickhouse.model

import java.time.LocalDate

data class ChProductPositionHistory(
    val date: LocalDate,
    val position: Long,
)
