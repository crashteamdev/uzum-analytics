package dev.crashteam.uzumanalytics.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotEmpty

@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "uzum")
data class UzumProperties(
    @field:NotEmpty
    val groupCron: String? = null,
    @field:NotEmpty
    val productCron: String? = null,
    @field:NotEmpty
    val paymentCron: String? = null,
    @field:NotEmpty
    val sellerCron: String? = null,
    @field:NotEmpty
    val reportCleanUpCron: String? = null,
    @field:NotEmpty
    val reportGenerateCron: String? = null,
    @field:NotEmpty
    val productPositionCron: String? = null,
    val throttlingMs: Long? = null,
)
