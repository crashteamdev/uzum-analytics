package dev.crashteam.uzumanalytics.client.qiwi.model

data class CreatePaymentResponse(
    val status: PaymentStatus,
    val payUrl: String,
)
