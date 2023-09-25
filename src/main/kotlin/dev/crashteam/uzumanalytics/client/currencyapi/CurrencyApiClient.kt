package dev.crashteam.uzumanalytics.client.currencyapi

import dev.crashteam.uzumanalytics.client.currencyapi.model.CurrencyApiResponse
import dev.crashteam.uzumanalytics.config.RedisConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange

@Component
class CurrencyApiClient(
    private val restTemplate: RestTemplate
) {

    @Value("\${currency.apiKey}")
    private lateinit var apiKey: String

    @Cacheable(value = [RedisConfig.CURRENCY_API_CACHE_NAME], unless = "#result == null")
    fun getCurrency(currencySymbolicCode: String = "RUB"): CurrencyApiResponse {
        val url = "https://api.currencyapi.com/v3/latest?apikey=$apiKey&currencies=$currencySymbolicCode"
        return restTemplate.exchange<CurrencyApiResponse>(url, HttpMethod.GET).body!!
    }
}
