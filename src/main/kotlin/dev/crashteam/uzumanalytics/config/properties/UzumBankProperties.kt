package dev.crashteam.uzumanalytics.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.core.io.Resource
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotEmpty

@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "uzumbank")
data class UzumBankProperties(
    @NotEmpty
    val baseUrl: String,
    val ssl: UzumBankSslProperties,
)

data class UzumBankSslProperties(
    val keyStore: Resource,
    val keyStorePassword: String,
    val keyPassword: String,
    val trustStore: Resource,
    val trustStorePassword: String,
)
