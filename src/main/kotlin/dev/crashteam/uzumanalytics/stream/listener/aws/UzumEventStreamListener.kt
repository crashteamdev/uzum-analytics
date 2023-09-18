package dev.crashteam.uzumanalytics.stream.listener.aws

import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput
import dev.crashteam.uzum.scrapper.data.v1.UzumScrapperEvent
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class UzumEventStreamListener : AwsStreamListener {

    override fun initialize(initializationInput: InitializationInput) {}

    override fun processRecords(processRecordsInput: ProcessRecordsInput) {
        val records = processRecordsInput.records
        val uzumScrapperEvents = records.map {
            UzumScrapperEvent.parseFrom(it.data)
        }
        log.info { "Consume uzum events records count: ${uzumScrapperEvents.size}" }

    }

    override fun shutdown(shutdownInput: ShutdownInput) {}
}
