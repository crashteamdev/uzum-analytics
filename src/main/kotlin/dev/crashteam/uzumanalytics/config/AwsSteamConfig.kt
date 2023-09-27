package dev.crashteam.uzumanalytics.config

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration.*
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker
import dev.crashteam.uzumanalytics.config.properties.AwsStreamProperties
import dev.crashteam.uzumanalytics.stream.listener.aws.UzumEventStreamProcessorFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class AwsSteamConfig(
    private val awsStreamProperties: AwsStreamProperties,
    private val uzumEventStreamProcessor: UzumEventStreamProcessorFactory
) {

    @Value("\${spring.application.name}")
    private lateinit var appName: String

    @Bean
    fun uzumStreamWorker(): Worker {
        val awsCredentials = BasicAWSCredentials(awsStreamProperties.accessKey, awsStreamProperties.secretKey)
        val consumerConfig = KinesisClientLibConfiguration(
            appName,
            awsStreamProperties.uzumStream.name,
            awsStreamProperties.endpoint,
            "",
            DEFAULT_INITIAL_POSITION_IN_STREAM,
            AWSStaticCredentialsProvider(awsCredentials),
            AWSStaticCredentialsProvider(awsCredentials),
            AWSStaticCredentialsProvider(awsCredentials),
            DEFAULT_FAILOVER_TIME_MILLIS,
            "$appName-consumer",
            DEFAULT_MAX_RECORDS,
            DEFAULT_IDLETIME_BETWEEN_READS_MILLIS,
            DEFAULT_DONT_CALL_PROCESS_RECORDS_FOR_EMPTY_RECORD_LIST,
            DEFAULT_PARENT_SHARD_POLL_INTERVAL_MILLIS,
            DEFAULT_SHARD_SYNC_INTERVAL_MILLIS,
            DEFAULT_CLEANUP_LEASES_UPON_SHARDS_COMPLETION,
            ClientConfiguration(),
            ClientConfiguration(),
            ClientConfiguration(),
            DEFAULT_TASK_BACKOFF_TIME_MILLIS,
            DEFAULT_METRICS_BUFFER_TIME_MILLIS,
            DEFAULT_METRICS_MAX_QUEUE_SIZE,
            DEFAULT_VALIDATE_SEQUENCE_NUMBER_BEFORE_CHECKPOINTING,
            awsStreamProperties.region,
            DEFAULT_SHUTDOWN_GRACE_MILLIS,
            DEFAULT_DDB_BILLING_MODE,
            null,
            0,
            0,
            0
        )

        return Worker.Builder()
            .recordProcessorFactory(uzumEventStreamProcessor)
            .config(consumerConfig)
            .build()
    }
}
