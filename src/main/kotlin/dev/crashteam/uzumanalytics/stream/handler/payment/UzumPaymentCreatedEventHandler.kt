package dev.crashteam.uzumanalytics.stream.handler.payment

import dev.crashteam.payment.PaymentEvent
import dev.crashteam.payment.UzumAnalyticsContext
import dev.crashteam.uzumanalytics.domain.mongo.PaymentDocument
import dev.crashteam.uzumanalytics.extension.toLocalDateTime
import dev.crashteam.uzumanalytics.repository.mongo.PaymentRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

@Component
class UzumPaymentCreatedEventHandler(
    private val paymentRepository: PaymentRepository,
    private val conversionService: ConversionService,
) : PaymentEventHandler {

    override fun handle(events: List<PaymentEvent>) {
        runBlocking {
            for (paymentEvent in events) {
                val paymentCreated = paymentEvent.payload.paymentChange.paymentCreated
                val paymentDocument = paymentRepository.findByPaymentId(paymentCreated.paymentId).awaitSingleOrNull()

                if (paymentDocument != null) continue

                val paymentStatus = conversionService.convert(paymentCreated.status, String::class.java)!!
                val newPaymentDocument = PaymentDocument(
                    paymentId = paymentCreated.paymentId,
                    userId = paymentCreated.userId,
                    createdAt = paymentCreated.createdAt.toLocalDateTime(),
                    status = paymentStatus,
                    paid = false,
                    amount = paymentCreated.amount.value.toBigDecimal().movePointLeft(2),
                    multiply = paymentCreated.userPaidService.paidService.context.multiply.toShort(),
                    subscriptionType = mapProtoSubscriptionPlan(
                        paymentCreated.userPaidService.paidService.context.uzumAnalyticsContext.plan
                    )
                )
                paymentRepository.save(newPaymentDocument).awaitSingleOrNull()
            }
        }
    }

    override fun isHandle(event: PaymentEvent): Boolean {
        return event.payload.hasPaymentChange() &&
                event.payload.paymentChange.hasPaymentCreated() &&
                event.payload.paymentChange.paymentCreated.hasUserPaidService() &&
                event.payload.paymentChange.paymentCreated.userPaidService.paidService.context.hasUzumAnalyticsContext()
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
