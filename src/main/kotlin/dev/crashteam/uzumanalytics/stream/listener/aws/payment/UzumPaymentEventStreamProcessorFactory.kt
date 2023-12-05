package dev.crashteam.uzumanalytics.stream.listener.aws.payment

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory
import dev.crashteam.uzumanalytics.stream.handler.payment.PaymentEventHandler
import org.springframework.stereotype.Component

@Component
class UzumPaymentEventStreamProcessorFactory(
    private val paymentEventHandler: List<PaymentEventHandler>
) : IRecordProcessorFactory {
    override fun createProcessor(): IRecordProcessor {
        return UzumPaymentEventStreamListener(paymentEventHandler)
    }
}
