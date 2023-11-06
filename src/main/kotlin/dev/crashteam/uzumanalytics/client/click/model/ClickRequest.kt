package dev.crashteam.uzumanalytics.client.click.model

import java.time.LocalDateTime

data class ClickRequest(
    val clickTransId: String,
    val serviceId: String,
    val clickPaydocId: String,
    val merchantTransId: String,
    val merchantPrepareId: String,
    val amount: String,
    val action: String,
    val error: String,
    val errorNote: String,
    val signTime: LocalDateTime,
    val rawSignTime: String,
    val signString: String,
    val referralCode: String?,
    val promoCode: String?,
    val promoCodeType: String?
) 