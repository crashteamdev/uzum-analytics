package dev.crashteam.uzumanalytics.client.uzumbank.model

data class UzumBankCreatePaymentResponse(
    override val errorCode: Int,
    override val message: String?,
) : UzumBankBaseResponse(errorCode, message)
