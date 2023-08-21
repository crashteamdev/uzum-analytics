package dev.crashteam.uzumanalytics.client.uzumbank.model

data class UzumBankCreatePaymentResponse(
    override val errorCode: Int,
    override val message: String?,
    val result: UzumBankCreatePaymentRedirect?,
) : UzumBankBaseResponse(errorCode, message)

data class UzumBankCreatePaymentRedirect(
    val orderId: String,
    val paymentRedirectUrl: String,
)
