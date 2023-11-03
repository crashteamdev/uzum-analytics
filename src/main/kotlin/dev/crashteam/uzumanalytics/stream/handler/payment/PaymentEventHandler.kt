package dev.crashteam.uzumanalytics.stream.handler.payment

import dev.crashteam.payment.PaymentEvent

interface PaymentEventHandler {

    fun handle(events: List<PaymentEvent>)

    fun isHandle(event: PaymentEvent): Boolean
}
