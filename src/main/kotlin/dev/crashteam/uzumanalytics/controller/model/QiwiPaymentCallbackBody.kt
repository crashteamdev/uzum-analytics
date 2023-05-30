package dev.crashteam.uzumanalytics.controller.model

import java.time.OffsetDateTime

data class QiwiPaymentCallbackBody(
    val payment: QiwiCallbackPayment,
    val type: String,
)

data class QiwiCallbackPayment(
    val paymentId: String,
    val status: QiwiCallbackPaymentStatus,
    val createdDateTime: String,
    val amount: QiwiCallbackPaymentAmount,
    val customer: QiwiCallbackPaymentCustomer,
    val customFields: Map<String, String>,
    val billId: String,
)

data class QiwiCallbackPaymentAmount(
    val value: String,
    val currency: String
)

data class QiwiCallbackPaymentStatus(
    val value: String,
    val changedDateTime: OffsetDateTime
)

data class QiwiCallbackPaymentCustomer(
    val account: String,
)
