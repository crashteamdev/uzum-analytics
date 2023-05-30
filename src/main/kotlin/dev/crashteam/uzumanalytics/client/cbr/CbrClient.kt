package dev.crashteam.uzumanalytics.client.cbr

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import dev.crashteam.uzumanalytics.client.cbr.model.CbrCurrencyRateResponse
import dev.crashteam.uzumanalytics.config.RedisConfig
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity

@Component
class CbrClient(
    private val restTemplate: RestTemplate,
    private val xmlMapper: XmlMapper,
) {

    @Cacheable(value = [RedisConfig.UZUM_CBR_CURRENCIES_CACHE_NAME], unless = "#result == null")
    fun getCurrencies(): CbrCurrencyRateResponse {
        val url = "https://www.cbr-xml-daily.ru/daily_eng_utf8.xml"
        val cbrXmlResponse = restTemplate.getForEntity<String>(url).body
        return xmlMapper.readValue(cbrXmlResponse, CbrCurrencyRateResponse::class.java)
    }
}
