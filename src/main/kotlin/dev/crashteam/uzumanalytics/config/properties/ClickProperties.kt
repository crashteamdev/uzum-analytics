package dev.crashteam.uzumanalytics.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "click")
data class ClickProperties(
    @field:NotEmpty
    val baseUrl: String? = null,
    @field:NotEmpty
    val merchantId: String? = null,
    @field:NotNull
    val serviceId: Long? = null,
    @field:NotEmpty
    val secretKey: String? = null,
    @field:NotEmpty
    val merchantUserId: String? = null
)
