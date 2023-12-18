package dev.crashteam.uzumanalytics.stream.handler.payment

import dev.crashteam.payment.PaymentCreated
import dev.crashteam.payment.PaymentEvent
import dev.crashteam.payment.PaymentStatus
import dev.crashteam.payment.UzumAnalyticsContext
import dev.crashteam.uzumanalytics.domain.mongo.PaymentDocument
import dev.crashteam.uzumanalytics.extension.toLocalDateTime
import dev.crashteam.uzumanalytics.repository.mongo.PaymentRepository
import dev.crashteam.uzumanalytics.service.PaymentService
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class UzumPaymentCreatedEventHandler(
    private val paymentRepository: PaymentRepository,
    private val paymentService: PaymentService,
) : PaymentEventHandler {

    override fun handle(events: List<PaymentEvent>) {
        runBlocking {
            for (paymentEvent in events) {
                val paymentCreated = paymentEvent.payload.paymentChange.paymentCreated
                val paymentDocument = paymentRepository.findByPaymentId(paymentCreated.paymentId).awaitSingleOrNull()

                if (paymentDocument != null) continue

                if (paymentCreated.status == PaymentStatus.PAYMENT_STATUS_SUCCESS) {
                    log.info { "Create payment with final state. paymentId=${paymentCreated.paymentId}; userId=${paymentCreated.userId}" }
                    createPayment(paymentCreated)
                    paymentService.callbackPayment(
                        paymentId = paymentCreated.paymentId,
                        userId = paymentCreated.userId,
                    )
                } else {
                    log.info { "Create payment. paymentId=${paymentCreated.paymentId}; userId=${paymentCreated.userId}" }
                    createPayment(paymentCreated)
                }
            }
        }
    }

    override fun isHandle(event: PaymentEvent): Boolean {
        return event.payload.hasPaymentChange() &&
                event.payload.paymentChange.hasPaymentCreated() &&
                event.payload.paymentChange.paymentCreated.hasUserPaidService() &&
                event.payload.paymentChange.paymentCreated.userPaidService.paidService.context.hasUzumAnalyticsContext()
    }

    private suspend fun createPayment(paymentCreated: PaymentCreated) {
        val newPaymentDocument = PaymentDocument(
            paymentId = paymentCreated.paymentId,
            userId = paymentCreated.userId,
            createdAt = paymentCreated.createdAt.toLocalDateTime(),
            status = mapPaymentStatus(paymentCreated.status),
            paid = false,
            amount = paymentCreated.amount.value.toBigDecimal().movePointLeft(2),
            multiply = paymentCreated.userPaidService.paidService.context.multiply.toShort(),
            subscriptionType = mapProtoSubscriptionPlan(
                paymentCreated.userPaidService.paidService.context.uzumAnalyticsContext.plan
            )
        )
        paymentRepository.save(newPaymentDocument).awaitSingleOrNull()
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

    private fun mapProtoSubscriptionPlan(uzumPlan: UzumAnalyticsContext.UzumAnalyticsPlan): Int {
        return when (uzumPlan.planCase) {
            UzumAnalyticsContext.UzumAnalyticsPlan.PlanCase.DEFAULT_PLAN -> 1
            UzumAnalyticsContext.UzumAnalyticsPlan.PlanCase.ADVANCED_PLAN -> 2
            UzumAnalyticsContext.UzumAnalyticsPlan.PlanCase.PRO_PLAN -> 3
            UzumAnalyticsContext.UzumAnalyticsPlan.PlanCase.PLAN_NOT_SET -> {
                throw IllegalStateException("Unknown paid service plan: $uzumPlan")
            }
        }
    }
}
