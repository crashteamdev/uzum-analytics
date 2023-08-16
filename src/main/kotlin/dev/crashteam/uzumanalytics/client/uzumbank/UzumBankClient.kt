package dev.crashteam.uzumanalytics.client.uzumbank

import dev.crashteam.uzumanalytics.client.uzumbank.model.UzumBankCreatePaymentRequest
import dev.crashteam.uzumanalytics.client.uzumbank.model.UzumBankCreatePaymentResponse
import dev.crashteam.uzumanalytics.config.properties.UzumBankProperties
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate

class UzumBankClient(
    private val uzumBankProperties: UzumBankProperties,
    private val uzumBankRestTemplate: RestTemplate,
) {

    fun createPayment(paymentRequest: UzumBankCreatePaymentRequest) {
        val httpHeaders = buildHeaders()
        val url = uzumBankProperties.baseUrl + "/api/v1/payment/register"
        val httpEntity = HttpEntity(paymentRequest, httpHeaders)
        val paymentResponseResponseEntity = uzumBankRestTemplate.exchange(
            url, HttpMethod.POST, httpEntity, UzumBankCreatePaymentResponse::class.java
        )
    }

    private fun buildHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set("Content-Language", "ru-RU")
            set("X-Fingerprint", "") // TODO
            set("X-Signature", "") // TODO
            set("X-Terminal-Id", "") // TODO
        }
    }

}
