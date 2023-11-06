package dev.crashteam.uzumanalytics.client.click.model

import dev.crashteam.uzumanalytics.domain.mongo.PromoCodeType
import java.math.BigDecimal

data class ClickPaymentFormRequestParams(
    val userId: String,
    val paymentId: String,
    val email: String,
    val amount: BigDecimal,
    val currency: String,
    val subscriptionId: Int,
    val referralCode: String? = null,
    val promoCode: String? = null,
    val promoCodeType: PromoCodeType? = null,
    val multiply: Short = 1
)
