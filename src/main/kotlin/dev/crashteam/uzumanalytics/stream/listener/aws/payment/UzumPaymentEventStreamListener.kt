package dev.crashteam.uzumanalytics.stream.listener.aws.payment

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason
import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput
import dev.crashteam.payment.PaymentEvent
import dev.crashteam.uzumanalytics.stream.handler.payment.PaymentEventHandler
import dev.crashteam.uzumanalytics.stream.listener.aws.AwsStreamListener
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class UzumPaymentEventStreamListener(
    private val paymentEventHandler: List<PaymentEventHandler>
) : AwsStreamListener {

    private var partitionId: String? = null

    override fun initialize(initializationInput: InitializationInput) {
        this.partitionId = initializationInput.shardId
        log.info { "[Uzum-Payment-Stream] Initialized partition $partitionId for streaming." }
    }

    override fun processRecords(processRecordsInput: ProcessRecordsInput) {
        val records = processRecordsInput.records
        log.debug {
            "Received uzum payment events. size=${processRecordsInput.records.size}"
        }

        List(records.size) { i: Int ->
            PaymentEvent.parseFrom(records[i].data)
        }.groupBy { entry -> paymentEventHandler.find { it.isHandle(entry) } }
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
        try {
            log.info { "[Uzum-Payment-Stream] Shutting down event processor for $partitionId" }
            if (shutdownInput.shutdownReason == ShutdownReason.TERMINATE) {
                shutdownInput.checkpointer.checkpoint()
            }
        } catch (e: Exception) {
            log.error(e) { "[Uzum-Payment-Stream] Failed to checkpoint on shutdown" }
        }
    }
}
