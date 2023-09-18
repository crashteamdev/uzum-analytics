package dev.crashteam.uzumanalytics.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "aws-stream")
data class AwsStreamProperties(
    val endpoint: String,
    val accessKey: String,
    val secretKey: String,
    val region: String,
    val uzumStream: UzumStreamProperties
)

data class UzumStreamProperties(
    val name: String
)
