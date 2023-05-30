package dev.crashteam.uzumanalytics.client.qiwi.model

import java.math.BigDecimal

data class QiwiPaymentRequestParams(
    val paymentId: String,
    val userId: String,
    val email: String,
    val amount: BigDecimal,
    val comment: String,
    val subscriptionId: Int,
    val referralCode: String? = null,
    val multiply: Short = 1,
    val currencySymbolicCode: String,
)
