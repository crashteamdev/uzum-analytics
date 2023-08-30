package dev.crashteam.uzumanalytics.config

import dev.crashteam.uzumanalytics.config.properties.UzumBankProperties
import org.apache.http.client.HttpClient
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustAllStrategy
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContexts
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.util.ResourceUtils
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate
import javax.net.ssl.SSLContext


@Configuration
class UzumBankHttpClientConfig(
    private val uzumBankProperties: UzumBankProperties
) {

    @Bean
    fun uzumBankRestTemplate(): RestTemplate? {
        val restTemplate = RestTemplate(clientHttpRequestFactory())
        restTemplate.errorHandler = object : DefaultResponseErrorHandler() {
            override fun hasError(statusCode: HttpStatus): Boolean {
                return false
            }
        }
        return restTemplate
    }

    private fun clientHttpRequestFactory(): ClientHttpRequestFactory {
        return HttpComponentsClientHttpRequestFactory(httpClient())
    }

    private fun httpClient(): HttpClient {
        val sslContext: SSLContext =
            SSLContexts.custom().loadTrustMaterial(
                null, TrustAllStrategy()
            ).loadKeyMaterial(
                ResourceUtils.getFile(uzumBankProperties.ssl.keyStore),
                uzumBankProperties.ssl.keyStorePassword.toCharArray(),
                uzumBankProperties.ssl.keyPassword.toCharArray()
            ).build()
        val sslConnectionSocketFactory = SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier())

        return HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build()
    }
}
