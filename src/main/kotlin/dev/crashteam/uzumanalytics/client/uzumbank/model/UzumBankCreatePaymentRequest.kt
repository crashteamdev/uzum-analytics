package dev.crashteam.uzumanalytics.client.uzumbank.model

data class UzumBankCreatePaymentRequest(
    val amount: Long,
    val clientId: String,
    val currency: Short,
    val orderNumber: String,
    val viewType: UzumBankViewType,
    val paymentParams: UzumBankPaymentParams,
    val sessionTimeoutSecs: Int,
    val successUrl: String?,
    val failureUrl: String?
)

data class UzumBankPaymentParams(
    val payType: UzumBankPayType
)

enum class UzumBankPayType {
    ONE_STEP, TWO_STEP
}
