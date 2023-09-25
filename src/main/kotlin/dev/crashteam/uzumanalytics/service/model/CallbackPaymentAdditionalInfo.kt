package dev.crashteam.uzumanalytics.service.model

import dev.crashteam.uzumanalytics.domain.mongo.PromoCodeType

data class CallbackPaymentAdditionalInfo(
    val promoCode: String,
    val promoCodeType: PromoCodeType,
)
