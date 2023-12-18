package dev.crashteam.uzumanalytics.stream.listener.redis.payment

import dev.crashteam.payment.PaymentEvent
import dev.crashteam.uzumanalytics.stream.handler.payment.PaymentEventHandler
import mu.KotlinLogging
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.stream.StreamListener
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class PaymentStreamListener(
    private val paymentEventHandlers: List<PaymentEventHandler>
) : StreamListener<String, ObjectRecord<String, ByteArray>> {

    override fun onMessage(message: ObjectRecord<String, ByteArray>) {
        try {
            val paymentEvent = PaymentEvent.parseFrom(message.value)
            log.info { "Listen payment event: $paymentEvent" }
            paymentEventHandlers.find { it.isHandle(paymentEvent) }?.handle(listOf(paymentEvent))
        } catch (e: Exception) {
            log.error(e) { "Exception during handle payment event" }
            throw e
        }
    }
}
