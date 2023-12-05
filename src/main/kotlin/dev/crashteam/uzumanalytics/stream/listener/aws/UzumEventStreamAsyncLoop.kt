package dev.crashteam.uzumanalytics.stream.listener.aws

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class UzumEventStreamAsyncLoop(
    private val uzumStreamWorker: Worker,
    private val paymentStreamWorker: Worker,
) {

    @Async
    fun startUzumDataStreamLoop() {
        uzumStreamWorker.run()
    }

    @Async
    fun startPaymentStreamLoop() {
        paymentStreamWorker.run()
    }

}
