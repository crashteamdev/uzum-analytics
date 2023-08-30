package dev.crashteam.uzumanalytics.client.uzumbank

import com.fasterxml.jackson.databind.ObjectMapper
import dev.crashteam.uzumanalytics.client.uzumbank.model.*
import dev.crashteam.uzumanalytics.config.properties.UzumBankProperties
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.io.File
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.cert.Certificate
import java.util.*

@Component
class UzumBankClient(
    private val uzumBankProperties: UzumBankProperties,
    private val uzumBankRestTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
) {

    fun createPayment(paymentRequest: UzumBankCreatePaymentRequest): UzumBankCreatePaymentResponse {
        val requestBody = objectMapper.writeValueAsString(paymentRequest)
        val httpHeaders = buildHeaders(requestBody)
        val httpEntity = HttpEntity(paymentRequest, httpHeaders)
        val url = uzumBankProperties.baseUrl + "/api/v1/payment/register"
        val paymentResponseResponseEntity = uzumBankRestTemplate.exchange(
            url, HttpMethod.POST, httpEntity, UzumBankCreatePaymentResponse::class.java
        )
        checkOnError(paymentResponseResponseEntity)

        return paymentResponseResponseEntity.body!!
    }

    fun getStatus(getStatusRequest: UzumBankGetStatusRequest): UzumBankGetStatusResponse {
        val requestBody = objectMapper.writeValueAsString(getStatusRequest)
        val httpHeaders = buildHeaders(requestBody)
        val httpEntity = HttpEntity(getStatusRequest, httpHeaders)
        val url = uzumBankProperties.baseUrl + "/api/v1/payment/getOrderStatus"
        val getStatusResponseResponseEntity = uzumBankRestTemplate.exchange(
            url, HttpMethod.POST, httpEntity, UzumBankGetStatusResponse::class.java
        )
        checkOnError(getStatusResponseResponseEntity)

        return getStatusResponseResponseEntity.body!!
    }

    private fun <T : UzumBankBaseResponse> checkOnError(response: ResponseEntity<T>) {
        val uzumBankBaseResponse = response.body!!
        if (uzumBankBaseResponse.errorCode != 0) {
            throw UzumBankClientException("Bad response. " +
                    "errorCode=${uzumBankBaseResponse.errorCode};" +
                    " message=${uzumBankBaseResponse.message}")
        }
    }

    private fun buildHeaders(requestBody: String): HttpHeaders {
        val keyStore = loadKeyStore(
            uzumBankProperties.ssl.keyStore.url,
            uzumBankProperties.ssl.keyStorePassword,
            "JKS"
        )
        val keyPair = getKeyPair(keyStore, "", "") // TODO
        val signature = generateSign(keyPair.private, keyPair.public, requestBody)
        return HttpHeaders().apply {
            set("Content-Language", "ru-RU")
            set("X-Fingerprint", uzumBankProperties.fingerprint)
            set("X-Signature", signature)
            set("X-Terminal-Id", uzumBankProperties.terminalId)
        }
    }

    private fun generateSign(privateKey: PrivateKey, publicKey: PublicKey, data: String): String {
        val ecdsaSign = Signature.getInstance("SHA256withECDSA")
        ecdsaSign.initSign(privateKey)
        ecdsaSign.update(data.toByteArray(StandardCharsets.UTF_8))
        val signature = ecdsaSign.sign()
        // TODO: delete after finalize integration
        // val pub: String = Base64.getEncoder().encodeToString(publicKey.encoded)

        return Base64.getEncoder().encodeToString(signature)
    }

    private fun loadKeyStore(
        keystoreUrl: URL,
        password: String,
        keyStoreType: String,
    ): KeyStore {
        val keystore = KeyStore.getInstance(keyStoreType)
        keystoreUrl.openStream().use {
            keystore.load(it, password.toCharArray())
        }
        return keystore
    }

    private fun getKeyPair(
        keystore: KeyStore,
        alias: String,
        password: String
    ): KeyPair {
        val key: Key = keystore.getKey(alias, password.toCharArray()) as PrivateKey
        val cert: Certificate = keystore.getCertificate(alias)
        val publicKey: PublicKey = cert.publicKey
        return KeyPair(publicKey, key as PrivateKey)
    }

}
