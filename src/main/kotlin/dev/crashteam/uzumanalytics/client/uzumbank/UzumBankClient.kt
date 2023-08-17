package dev.crashteam.uzumanalytics.client.uzumbank

import com.fasterxml.jackson.databind.ObjectMapper
import dev.crashteam.uzumanalytics.client.uzumbank.model.UzumBankCreatePaymentRequest
import dev.crashteam.uzumanalytics.client.uzumbank.model.UzumBankCreatePaymentResponse
import dev.crashteam.uzumanalytics.config.properties.UzumBankProperties
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.io.File
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

        return paymentResponseResponseEntity.body!!
    }

    private fun buildHeaders(requestBody: String): HttpHeaders {
        val keyStore = loadKeyStore(
            uzumBankProperties.ssl.keyStore.file,
            uzumBankProperties.ssl.keyStorePassword,
            "JKS"
        )
        val keyPair = getKeyPair(keyStore, "", "") // TODO
        val publicKey: PublicKey = keyPair.public
        val privateKey: PrivateKey = keyPair.private
        val signature = generateSign(keyPair.private, keyPair.public, requestBody)
        return HttpHeaders().apply {
            set("Content-Language", "ru-RU")
            set("X-Fingerprint", "") // TODO
            set("X-Signature", signature)
            set("X-Terminal-Id", uzumBankProperties.terminalId)
        }
    }

    private fun generateSign(privateKey: PrivateKey, publicKey: PublicKey, data: String): String {
        val ecdsaSign = Signature.getInstance("SHA256withECDSA")
        ecdsaSign.initSign(privateKey)
        ecdsaSign.update(data.toByteArray(StandardCharsets.UTF_8))
        val signature = ecdsaSign.sign()
        val pub: String = Base64.getEncoder().encodeToString(publicKey.encoded)

        return Base64.getEncoder().encodeToString(signature)
    }

    private fun loadKeyStore(
        keystoreFile: File,
        password: String,
        keyStoreType: String,
    ): KeyStore {
        val keystoreUri = keystoreFile.toURI()
        val keystoreUrl = keystoreUri.toURL()
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
