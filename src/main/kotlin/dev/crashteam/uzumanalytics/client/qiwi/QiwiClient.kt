package dev.crashteam.uzumanalytics.client.qiwi

import dev.crashteam.uzumanalytics.client.qiwi.model.*
import dev.crashteam.uzumanalytics.config.properties.QiwiProperties
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.time.ZoneId

@Component
class QiwiClient(
    private val qiwiProperties: QiwiProperties,
    private val restTemplate: RestTemplate
) {

    fun createPayment(paymentParams: QiwiPaymentRequestParams): CreatePaymentResponse {
        val url = "$ROOT_URL/payin/v1/sites/${qiwiProperties.siteId}/bills/${paymentParams.paymentId}"
        val httpHeaders = HttpHeaders().apply {
            set(HttpHeaders.AUTHORIZATION, "Bearer ${qiwiProperties.apiKey}")
            set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        }
        val createPaymentBody = CreatePaymentBody(
            amount = PaymentAmount(
                value =  paymentParams.amount.setScale(2, RoundingMode.HALF_UP).toString(),
                currency = paymentParams.currencySymbolicCode
            ),
            expirationDateTime = OffsetDateTime.now(ZoneId.of("Europe/Moscow")).plusMinutes(30),
            comment = paymentParams.comment,
            billPaymentMethodsType = listOf("CARD", "QIWI_WALLET"),
            flags = listOf("SALE"),
            customer = PaymentCustomerInfo(
                account = paymentParams.userId,
                email = paymentParams.email
            ),
            customFields = mapOf(
                "sub_id" to paymentParams.subscriptionId.toString(),
                "referral_code" to (paymentParams.referralCode ?: ""),
                "multiply" to paymentParams.multiply.toString()
            )
        )
        val httpEntity = HttpEntity(createPaymentBody, httpHeaders)
        val responseEntity = restTemplate.exchange<CreatePaymentResponse>(url, HttpMethod.PUT, httpEntity)

        return responseEntity.body!!
    }

    companion object {
        const val ROOT_URL = "https://api.qiwi.com/partner"
    }
}
