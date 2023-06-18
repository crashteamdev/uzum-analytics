package dev.crashteam.uzumanalytics.stream.scheduler

import dev.crashteam.uzumanalytics.config.properties.RedisProperties
import dev.crashteam.uzumanalytics.extensions.getApplicationContext
import dev.crashteam.uzumanalytics.stream.listener.BatchStreamListener
import dev.crashteam.uzumanalytics.stream.listener.UzumCategoryStreamListener
import dev.crashteam.uzumanalytics.stream.listener.UzumProductItemStreamListener
import dev.crashteam.uzumanalytics.stream.listener.UzumProductPositionStreamListener
import dev.crashteam.uzumanalytics.stream.service.PendingMessageService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.stream.StreamListener

private val log = KotlinLogging.logger {}

@DisallowConcurrentExecution
class PendingMessageScheduler : Job {

    override fun execute(context: JobExecutionContext) {
        val appContext = context.getApplicationContext()
        val pendingMessageService = appContext.getBean(PendingMessageService::class.java)
        val redisProperties = appContext.getBean(RedisProperties::class.java)
        val uzumProductItemStreamListener = appContext.getBean(UzumProductItemStreamListener::class.java)
        val uzumProductPositionStreamListener = appContext.getBean(UzumProductPositionStreamListener::class.java)
        val uzumCategoryStreamListener = appContext.getBean(UzumCategoryStreamListener::class.java)
        runBlocking {
            val productPendingMessageTask = async {
                processPendingMessage(
                    streamKey = redisProperties.stream.keProductInfo.streamName,
                    consumerGroup = redisProperties.stream.keProductInfo.consumerGroup,
                    consumerName = redisProperties.stream.keProductInfo.consumerName,
                    batchListener = uzumProductItemStreamListener,
                    pendingMessageService
                )
            }
            val productPositionPendingMessageTask = async {
                processPendingMessage(
                    streamKey = redisProperties.stream.keProductPosition.streamName,
                    consumerGroup = redisProperties.stream.keProductPosition.consumerGroup,
                    consumerName = redisProperties.stream.keProductPosition.consumerName,
                    listener = uzumProductPositionStreamListener,
                    pendingMessageService
                )
            }
            val categoryPendingMessageTask = async {
                processPendingMessage(
                    streamKey = redisProperties.stream.keCategoryInfo.streamName,
                    consumerGroup = redisProperties.stream.keCategoryInfo.consumerGroup,
                    consumerName = redisProperties.stream.keCategoryInfo.consumerName,
                    listener = uzumCategoryStreamListener,
                    pendingMessageService
                )
            }
            awaitAll(productPendingMessageTask, productPositionPendingMessageTask, categoryPendingMessageTask)
        }
    }

    private suspend fun processPendingMessage(
        streamKey: String,
        consumerGroup: String,
        consumerName: String,
        batchListener: BatchStreamListener<String, ObjectRecord<String, String>>,
        pendingMessageService: PendingMessageService
    ) {
        try {
            log.info { "Processing pending message by consumer $consumerName" }
            pendingMessageService.receiveBatchPendingMessages(
                streamKey = streamKey,
                consumerGroupName = consumerGroup,
                consumerName = consumerName,
                listener = batchListener
            )
        } catch (e: Exception) {
            log.error(e) { "Processing pending message failed" }
        }
    }

    private suspend fun processPendingMessage(
        streamKey: String,
        consumerGroup: String,
        consumerName: String,
        listener: StreamListener<String, ObjectRecord<String, String>>,
        pendingMessageService: PendingMessageService
    ) {
        try {
            log.info { "Processing pending message by consumer $consumerName" }
            pendingMessageService.receivePendingMessages(
                streamKey = streamKey,
                consumerGroupName = consumerGroup,
                consumerName = consumerName,
                listener = listener,
            )
        } catch (e: Exception) {
            log.error(e) { "Processing pending message failed" }
        }
    }
}
