package dev.crashteam.uzumanalytics.service.model

import dev.crashteam.uzumanalytics.domain.mongo.PromoCodeType
import java.time.LocalDateTime

data class PromoCodeCreateData(
    val description: String,
    val validUntil: LocalDateTime,
    val useLimit: Int,
    val type: PromoCodeType,
    val discount: Short? = null,
    val additionalDays: Int? = null,
    val prefix: String? = null,
)
