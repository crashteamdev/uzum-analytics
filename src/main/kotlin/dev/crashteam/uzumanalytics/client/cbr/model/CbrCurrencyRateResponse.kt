package dev.crashteam.uzumanalytics.client.cbr.model

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class CbrCurrencyRateResponse(
    @field:JacksonXmlProperty(localName = "Valute")
    val currencies: List<CbrCurrency>,
)

data class CbrCurrency(
    @field:JacksonXmlProperty(localName = "NumCode")
    val code: Int,
    @field:JacksonXmlProperty(localName = "CharCode")
    val charCode: String,
    @field:JacksonXmlProperty(localName = "Nominal")
    val nominal: String,
    @field:JacksonXmlProperty(localName = "Value")
    val value: String,
)
