package dev.crashteam.uzumanalytics.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "aws-stream")
data class AwsStreamProperties(
    val kinesisEndpoint: String,
    val dinamoDbEndpoint: String,
    val accessKey: String,
    val secretKey: String,
    val region: String,
    val uzumStream: UzumStreamProperties,
    val timeoutInSec: Int,
    val maxRecords: Int,
)

data class UzumStreamProperties(
    val name: String
)
