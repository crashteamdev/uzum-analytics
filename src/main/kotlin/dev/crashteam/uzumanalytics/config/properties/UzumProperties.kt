package dev.crashteam.uzumanalytics.config.properties

import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "uzum")
data class UzumProperties(
    @field:NotEmpty
    val groupCron: String? = null,
    @field:NotEmpty
    val productCron: String? = null,
    @field:NotEmpty
    val aggregateCron: String? = null,
    @field:NotEmpty
    val sellerCron: String? = null,
    @field:NotEmpty
    val reportCleanUpCron: String? = null,
    @field:NotEmpty
    val reportGenerateCron: String? = null,
    @field:NotEmpty
    val productPositionCron: String? = null,
    @field:NotEmpty
    val pendingMessageCron: String? = null,
    val throttlingMs: Long? = null,
    val apiLimit: UzumApiLimitProperties
)

data class UzumApiLimitProperties(
    val maxIp: Int,
    val maxBrowser: Int,
    val blockRemoveHour: Int,
)
