package dev.crashteam.uzumanalytics.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotEmpty

@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "qiwi")
data class QiwiProperties(
    @field:NotEmpty
    val siteId: String,
    @field:NotEmpty
    val apiKey: String,
    @field:NotEmpty
    val publicKey: String? = null,
    @field:NotEmpty
    val callbackSecret: String
)
