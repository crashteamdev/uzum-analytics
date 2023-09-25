package dev.crashteam.uzumanalytics.client.uzumbank.model

import java.math.BigDecimal

data class UzumBankGetStatusResponse(
    override val errorCode: Int,
    override val message: String?,
    val result: UzumBankGetStatusResult
) : UzumBankBaseResponse(errorCode, message)

data class UzumBankGetStatusResult(
    val orderId: String,
    val status: String,
    val merchantOrderId: String,
    val totalAmount: BigDecimal,
)
