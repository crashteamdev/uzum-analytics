package dev.crashteam.uzumanalytics.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotEmpty

@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "actuator")
data class ActuatorProperties(
    @field:NotEmpty
    val user: String? = null,
    @field:NotEmpty
    val password: String? = null,
)
