package dev.crashteam.uzumanalytics.client.freekassa

import com.fasterxml.jackson.databind.ObjectMapper
import dev.crashteam.uzumanalytics.client.freekassa.model.FreeKassaPaymentRequestParams
import dev.crashteam.uzumanalytics.client.freekassa.model.PaymentFormRequestParams
import dev.crashteam.uzumanalytics.client.freekassa.model.PaymentResponse
import dev.crashteam.uzumanalytics.config.properties.FreeKassaProperties
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class FreeKassaClient(
    private val freeKassaProperties: FreeKassaProperties,
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper
) {

    fun createPayment(payment: FreeKassaPaymentRequestParams): PaymentResponse {
        val url = freeKassaProperties.baseUrl + "/v1/orders/create"
        val requestParams = mutableMapOf(
            "shopId" to freeKassaProperties.shopId!!,
            "nonce" to payment.orderId,
            "paymentId" to payment.paymentId,
            "i" to payment.paymentSystemIdentifier,
            "email" to payment.email,
            "ip" to payment.ip,
            "amount" to payment.amount,
            "currency" to payment.currency
        )
        val sign = generateSign(requestParams)
        requestParams["signature"] = sign
        val requestBody = objectMapper.writeValueAsString(requestParams)

        return restTemplate.exchange(url, HttpMethod.POST, HttpEntity(requestBody), PaymentResponse::class.java).body!!
    }

    fun createPaymentFormUrl(paymentFormRequest: PaymentFormRequestParams): String {
        val sign = DigestUtils.md5Hex(
            "${freeKassaProperties.shopId}:${paymentFormRequest.amount}" +
                    ":${freeKassaProperties.secretWordFirst}:${paymentFormRequest.currency}:${paymentFormRequest.email}"
        )

        return buildString {
            append("$FREEKASSA_BASE_URL/?m=${freeKassaProperties.shopId}&oa=${paymentFormRequest.amount}")
            append("&currency=${paymentFormRequest.currency}&o=${paymentFormRequest.email}&pay=PAY&s=$sign")
            append("&em=${paymentFormRequest.email}")
            append("&us_paymentid=${paymentFormRequest.orderId}")
            append("&us_userid=${paymentFormRequest.userId}")
            append("&us_subscriptionid=${paymentFormRequest.subscriptionId}")
            append("&us_multiply=${paymentFormRequest.multiply}")
            if (paymentFormRequest.referralCode != null) {
                append("&us_referralcode=${paymentFormRequest.referralCode}")
            }
        }
    }

    private fun generateSign(requestParams: Map<String, Comparable<*>>): String {
        val concatValues = requestParams.toSortedMap().values.joinToString(separator = "|")
        val signingKey = SecretKeySpec(freeKassaProperties.apiKey!!.encodeToByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(signingKey)
        val rawHmac: ByteArray = mac.doFinal(concatValues.encodeToByteArray())
        val hexArray = byteArrayOf(
            '0'.code.toByte(),
            '1'.code.toByte(),
            '2'.code.toByte(),
            '3'.code.toByte(),
            '4'.code.toByte(),
            '5'.code.toByte(),
            '6'.code.toByte(),
            '7'.code.toByte(),
            '8'.code.toByte(),
            '9'.code.toByte(),
            'a'.code.toByte(),
            'b'.code.toByte(),
            'c'.code.toByte(),
            'd'.code.toByte(),
            'e'.code.toByte(),
            'f'.code.toByte()
        )
        val hexChars = ByteArray(rawHmac.size * 2)
        for (j in rawHmac.indices) {
            val v = rawHmac[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }

        return String(hexChars)
    }

    companion object {
        const val FREEKASSA_BASE_URL = "https://pay.freekassa.ru"
    }

}
