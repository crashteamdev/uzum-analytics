package dev.crashteam.uzumanalytics.stream.handler.payment

import dev.crashteam.payment.PaymentEvent
import dev.crashteam.payment.PaymentStatus
import dev.crashteam.uzumanalytics.repository.mongo.PaymentRepository
import dev.crashteam.uzumanalytics.service.PaymentService
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class UzumPaymentStatusEventHandler(
    private val paymentService: PaymentService,
    private val paymentRepository: PaymentRepository,
) : PaymentEventHandler {

    override fun handle(events: List<PaymentEvent>) {
        runBlocking {
            for (event in events) {
                val paymentStatusChanged = event.payload.paymentChange.paymentStatusChanged
                val paymentDocument =
                    paymentRepository.findByPaymentId(paymentStatusChanged.paymentId).awaitSingleOrNull()
                        ?: continue
                val paymentStatus = mapPaymentStatus(paymentStatusChanged.status)
                val updatePaymentDocument = paymentDocument.copy(status = paymentStatus)
                when (paymentStatusChanged.status) {
                    PaymentStatus.PAYMENT_STATUS_PENDING,
                    PaymentStatus.PAYMENT_STATUS_CANCELED,
                    PaymentStatus.PAYMENT_STATUS_FAILED -> {
                        log.warn { "Failed payment: $updatePaymentDocument" }
                        paymentRepository.save(updatePaymentDocument).awaitSingleOrNull()
                    }

                    PaymentStatus.PAYMENT_STATUS_SUCCESS -> {
                        log.info { "Success payment. paymentId=${paymentStatusChanged.paymentId}" }
                        if (paymentDocument.status != "success") {
                            paymentService.callbackPayment(
                                paymentId = paymentStatusChanged.paymentId,
                                userId = paymentDocument.userId,
                            )
                        }
                    }

                    PaymentStatus.PAYMENT_STATUS_UNKNOWN, PaymentStatus.UNRECOGNIZED -> {
                        log.warn { "Received payment event with unknown status: ${paymentStatusChanged.status}" }
                    }
                }
            }
        }
    }

    override fun isHandle(event: PaymentEvent): Boolean {
        return event.payload.hasPaymentChange() && event.payload.paymentChange.hasPaymentStatusChanged()
    }

    private fun mapPaymentStatus(paymentStatus: PaymentStatus): String {
        return when (paymentStatus) {
            PaymentStatus.PAYMENT_STATUS_PENDING -> "pending"
            PaymentStatus.PAYMENT_STATUS_SUCCESS -> "success"
            PaymentStatus.PAYMENT_STATUS_CANCELED -> "canceled"
            PaymentStatus.PAYMENT_STATUS_FAILED -> "failed"
            PaymentStatus.PAYMENT_STATUS_UNKNOWN, PaymentStatus.UNRECOGNIZED -> "unknown"
        }
    }
}
