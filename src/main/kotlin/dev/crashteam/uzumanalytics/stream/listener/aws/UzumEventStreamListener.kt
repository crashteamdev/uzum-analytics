package dev.crashteam.uzumanalytics.stream.listener.aws

import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput
import dev.crashteam.uzum.scrapper.data.v1.UzumScrapperEvent
import dev.crashteam.uzumanalytics.stream.handler.UzumScrapEventHandler
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
        val uzumScrapperEvents = records.map {
            UzumScrapperEvent.parseFrom(it.data)
        }
        List(records.size) { i: Int ->
            UzumScrapperEvent.parseFrom(records[i].data)
        }.groupBy { entry -> uzumScrapEventHandlers.find { it.isHandle(entry) } }
            .forEach { (handler, entries) ->
                handler?.handle(entries)
            }
        log.info { "Consume uzum events records count: ${uzumScrapperEvents.size}" }

    }

    override fun shutdown(shutdownInput: ShutdownInput) {}
}
