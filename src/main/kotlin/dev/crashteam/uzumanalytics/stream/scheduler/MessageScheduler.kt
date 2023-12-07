package dev.crashteam.uzumanalytics.stream.scheduler

import dev.crashteam.uzumanalytics.config.properties.RedisProperties
import dev.crashteam.uzumanalytics.stream.listener.redis.payment.PaymentStreamListener
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.stream.StreamListener
import org.springframework.data.redis.stream.StreamReceiver
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers
import reactor.util.retry.Retry
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.annotation.PostConstruct

private val log = KotlinLogging.logger {}

@Component
class MessageScheduler(
    private val paymentSubscription: StreamReceiver<String, ObjectRecord<String, ByteArray>>,
    private val paymentStreamListener: PaymentStreamListener,
    private val redisProperties: RedisProperties,
    private val messageReactiveRedisTemplate: ReactiveRedisTemplate<String, String>,
    private val retryTemplate: RetryTemplate,
) {

    val executor: ExecutorService = Executors.newSingleThreadExecutor()

    @PostConstruct
    fun receiveMessages() {
        executor.submit {
            retryTemplate.execute<Unit, Exception> {
                runBlocking {
                    log.info { "Start receiving stream messages" }
                    try {
                        val paymentConsumerTask = async {
                            createConsumer(
                                redisProperties.stream.payment.streamName,
                                redisProperties.stream.payment.consumerGroup,
                                redisProperties.stream.payment.consumerName,
                                paymentSubscription,
                                paymentStreamListener,
                            )
                        }
                        awaitAll(paymentConsumerTask)
                    } catch (e: Exception) {
                        log.error(e) { "Exception during creating consumers" }
                        throw e
                    }
                    log.info { "End of receiving stream messages" }
                }
            }
        }
    }

    private fun <V> createConsumer(
        streamKey: String,
        consumerGroup: String,
        consumerName: String,
        receiver: StreamReceiver<String, ObjectRecord<String, V>>,
        listener: StreamListener<String, ObjectRecord<String, V>>,
    ) {
        val consumer = Consumer.from(consumerGroup, consumerName)
        receiver.receive(
            consumer,
            StreamOffset.create(streamKey, ReadOffset.lastConsumed())
        ).publishOn(Schedulers.boundedElastic()).doOnNext {
            listener.onMessage(it)
            messageReactiveRedisTemplate.opsForStream<String, String>().acknowledge(streamKey, consumerGroup, it.id)
                .subscribe()
        }.retryWhen(
            Retry.fixedDelay(MAX_RETRY_ATTEMPTS, java.time.Duration.ofSeconds(RETRY_DURATION_SEC)).doBeforeRetry {
                log.warn(it.failure()) { "Error during consumer task" }
            }).subscribe()
    }

    private companion object {
        const val MAX_RETRY_ATTEMPTS = 30L
        const val RETRY_DURATION_SEC = 60L
    }
}
