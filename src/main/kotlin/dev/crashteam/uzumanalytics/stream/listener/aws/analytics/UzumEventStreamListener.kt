package dev.crashteam.uzumanalytics.stream.listener.aws.analytics

import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput
import dev.crashteam.uzum.scrapper.data.v1.UzumScrapperEvent
import dev.crashteam.uzumanalytics.stream.handler.analytics.UzumScrapEventHandler
import dev.crashteam.uzumanalytics.stream.listener.aws.AwsStreamListener
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class UzumEventStreamListener(
    private val uzumScrapEventHandlers: List<UzumScrapEventHandler>
) : AwsStreamListener {

    override fun initialize(initializationInput: InitializationInput) {}

    override fun processRecords(processRecordsInput: ProcessRecordsInput) {
        val records = processRecordsInput.records
        log.debug {
            "Received uzum scrap events. size=${processRecordsInput.records.size}"
        }
        List(records.size) { i: Int ->
            UzumScrapperEvent.parseFrom(records[i].data)
        }.groupBy { entry -> uzumScrapEventHandlers.find { it.isHandle(entry) } }
            .forEach { (handler, entries) ->
                try {
                    handler?.handle(entries)
                } catch (e: Exception) {
                    log.error(e) { "Failed to handle event" }
                }
            }
        try {
            log.info { "Consume uzum events records count: ${records.size}" }
            processRecordsInput.checkpointer.checkpoint()
        } catch (e: Exception) {
            log.error(e) { "Failed to checkpoint consumed records" }
        }
    }

    override fun shutdown(shutdownInput: ShutdownInput) {
            shutdownInput.checkpointer.checkpoint()
    }
}
