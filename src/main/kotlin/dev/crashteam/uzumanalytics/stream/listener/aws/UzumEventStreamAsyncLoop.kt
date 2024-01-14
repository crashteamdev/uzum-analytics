package dev.crashteam.uzumanalytics.stream.listener.aws

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker
import dev.crashteam.uzumanalytics.stream.listener.aws.model.RestartPaymentStreamEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class UzumEventStreamAsyncLoop(
    private var uzumStreamWorker: Worker,
    private var paymentStreamWorker: Worker,
    private val applicationContext: ApplicationContext,
) {

    @Async
    fun startUzumDataStreamLoop() {
        uzumStreamWorker.run()
    }

    @Async
    fun startPaymentStreamLoop() {
        paymentStreamWorker.run()
    }

    @EventListener
    fun restartPaymentStream(restartPaymentStreamEvent: RestartPaymentStreamEvent) {
        paymentStreamWorker.shutdown()
        paymentStreamWorker = applicationContext.getBean("paymentStreamWorker", Worker::class.java)
        paymentStreamWorker.run()
    }

}
