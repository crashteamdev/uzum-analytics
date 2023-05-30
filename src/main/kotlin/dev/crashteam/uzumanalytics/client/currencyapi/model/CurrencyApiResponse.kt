package dev.crashteam.uzumanalytics.client.currencyapi.model

import java.math.BigDecimal

data class CurrencyApiResponse(
    val data: Map<String, CurrencyApiData>
)

data class CurrencyApiData(
    val code: String,
    val value: BigDecimal
)
