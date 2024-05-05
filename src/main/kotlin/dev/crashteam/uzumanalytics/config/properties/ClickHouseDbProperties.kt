package dev.crashteam.uzumanalytics.config.properties

import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "clickhouse")
data class ClickHouseDbProperties(
    @field:NotEmpty
    val url: String? = null,
    @field:NotEmpty
    val user: String? = null,
    val password: String? = null,
    val connectionTimeout: Long = 120000,
    val socketTimeout: Long = 60000,
    val compress: Boolean = false,
    val ssl: Boolean = false,
)
