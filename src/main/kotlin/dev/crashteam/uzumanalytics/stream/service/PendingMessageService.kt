package dev.crashteam.uzumanalytics.stream.service

import org.springframework.data.redis.connection.stream.PendingMessage
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mu.KotlinLogging
import org.springframework.data.domain.Range
import org.springframework.data.redis.connection.ReactiveRedisConnection
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.stream.StreamListener
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.time.Duration

private val log = KotlinLogging.logger {}

@Component
class PendingMessageService(
    private val messageReactiveRedisTemplate: ReactiveRedisTemplate<String, String>
) {

    suspend fun <V> receivePendingMessages(
        streamKey: String,
        consumerGroupName: String,
        consumerName: String,
        listener: StreamListener<String, ObjectRecord<String, V>>,
        targetType: Class<V>,
    ) {
        messageReactiveRedisTemplate.opsForStream<String, V>().pending(
            streamKey,
            consumerGroupName,
            Range.unbounded<String>(),
            MAX_NUMBER_FETCH
        ).awaitSingleOrNull()?.forEach { pendingMessage ->
            claimMessage(streamKey, consumerGroupName, consumerName, pendingMessage)
            log.info { "Message: ${pendingMessage.idAsString} has been claimed by $consumerGroupName:$consumerName." +
                    " pendingMessage=${pendingMessage}" }
            val messagesToProcess = messageReactiveRedisTemplate.opsForStream<String, V>().range(
                targetType,
                streamKey,
                Range.closed(pendingMessage.idAsString, pendingMessage.idAsString)
            ).collectList().awaitSingleOrNull()
            if (messagesToProcess.isNullOrEmpty()) {
                log.warn {
                    "Message is not present." +
                            " It has been either processed or deleted by some other process: ${pendingMessage.idAsString}"
                }
            } else if (pendingMessage.totalDeliveryCount > MAX_RETRY) {
                messageReactiveRedisTemplate.opsForStream<String, V>()
                    .acknowledge(streamKey, consumerGroupName, pendingMessage.idAsString).awaitSingleOrNull()
                log.info { "Message has been added acknowledged case of max_retry attempts: ${pendingMessage.idAsString}" }
            } else {
                for (message in messagesToProcess) {
                    listener.onMessage(message)
                    messageReactiveRedisTemplate.opsForStream<String, V>()
                        .acknowledge(streamKey, consumerGroupName, message.id).awaitSingleOrNull()
                }
            }
        }
    }

    private suspend fun claimMessage(
        streamKey: String,
        consumerGroupName: String,
        consumerName: String,
        pendingMessage: PendingMessage
    ) {
        val redisConnection: ReactiveRedisConnection = messageReactiveRedisTemplate.connectionFactory.reactiveConnection
        redisConnection.streamCommands().xClaim(
            ByteBuffer.wrap(streamKey.toByteArray()),
            consumerGroupName,
            consumerName,
            Duration.ofSeconds(5),
            pendingMessage.id
        ).awaitFirstOrNull()
    }

    private companion object {
        const val MAX_NUMBER_FETCH = 500L
        const val MAX_RETRY = 5
    }
}
