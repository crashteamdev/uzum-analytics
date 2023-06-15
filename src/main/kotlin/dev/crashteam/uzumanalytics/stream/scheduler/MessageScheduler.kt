package dev.crashteam.uzumanalytics.stream.scheduler

import dev.crashteam.uzumanalytics.config.properties.RedisProperties
import dev.crashteam.uzumanalytics.stream.listener.BatchStreamListener
import dev.crashteam.uzumanalytics.stream.listener.KeCategoryStreamListener
import dev.crashteam.uzumanalytics.stream.listener.KeProductItemStreamListener
import dev.crashteam.uzumanalytics.stream.listener.KeProductPositionStreamListener
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
    private val uzumProductPositionSubscription: StreamReceiver<String, ObjectRecord<String, String>>,
    private val uzumProductSubscription: StreamReceiver<String, ObjectRecord<String, String>>,
    private val uzumCategorySubscription: StreamReceiver<String, ObjectRecord<String, String>>,
    private val keProductPositionStreamListener: KeProductPositionStreamListener,
    private val keProductStreamListener: KeProductItemStreamListener,
    private val keCategoryStreamListener: KeCategoryStreamListener,
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
                        val createPositionConsumerTask = async {
                            createConsumer(
                                redisProperties.stream.keProductPosition.streamName,
                                redisProperties.stream.keProductPosition.consumerGroup,
                                redisProperties.stream.keProductPosition.consumerName,
                                uzumProductPositionSubscription,
                                keProductPositionStreamListener
                            )
                        }
                        val createProductConsumerTask = async {
                            creatBatchConsumer(
                                redisProperties.stream.keProductInfo.streamName,
                                redisProperties.stream.keProductInfo.consumerGroup,
                                redisProperties.stream.keProductInfo.consumerName,
                                uzumProductSubscription,
                                keProductStreamListener
                            )
                        }
                        val createCategoryConsumerTask = async {
                            createConsumer(
                                redisProperties.stream.keCategoryInfo.streamName,
                                redisProperties.stream.keCategoryInfo.consumerGroup,
                                redisProperties.stream.keCategoryInfo.consumerName,
                                uzumCategorySubscription,
                                keCategoryStreamListener
                            )
                        }
                        awaitAll(createPositionConsumerTask, createProductConsumerTask, createCategoryConsumerTask)
                    } catch (e: Exception) {
                        log.error(e) { "Exception during creating consumers" }
                        throw e
                    }
                    log.info { "End of receiving stream messages" }
                }
            }
        }
    }

    private fun createConsumer(
        streamKey: String,
        consumerGroup: String,
        consumerName: String,
        receiver: StreamReceiver<String, ObjectRecord<String, String>>,
        listener: StreamListener<String, ObjectRecord<String, String>>,
    ) {
        val consumer = Consumer.from(consumerGroup, consumerName)
        receiver.receiveAutoAck(
            consumer,
            StreamOffset.create(streamKey, ReadOffset.lastConsumed())
        ).publishOn(Schedulers.boundedElastic()).doOnNext {
            listener.onMessage(it)
        }.retryWhen(
            Retry.fixedDelay(MAX_RETRY_ATTEMPTS, java.time.Duration.ofSeconds(RETRY_DURATION_SEC)).doBeforeRetry {
                log.warn(it.failure()) { "Error during consumer task" }
            }).subscribe()
    }

    private fun creatBatchConsumer(
        streamKey: String,
        consumerGroup: String,
        consumerName: String,
        receiver: StreamReceiver<String, ObjectRecord<String, String>>,
        listener: BatchStreamListener<String, ObjectRecord<String, String>>,
    ) {
        val consumer = Consumer.from(consumerGroup, consumerName)
        receiver.receiveAutoAck(
            consumer,
            StreamOffset.create(streamKey, ReadOffset.lastConsumed())
        ).bufferTimeout(
            redisProperties.stream.maxBatchSize,
            java.time.Duration.ofMillis(redisProperties.stream.batchBufferDurationMs)
        ).onBackpressureBuffer()
            .parallel(redisProperties.stream.batchParallelCount)
            .runOn(Schedulers.newParallel("redis-stream-batch", redisProperties.stream.batchParallelCount))
            .doOnNext { records ->
                runBlocking {
                    listener.onMessage(records)
                }
            }.doOnError {
                log.warn(it) { "Error during consumer task" }
            }.subscribe()
    }

    private companion object {
        const val MAX_RETRY_ATTEMPTS = 30L
        const val RETRY_DURATION_SEC = 60L
    }
}
