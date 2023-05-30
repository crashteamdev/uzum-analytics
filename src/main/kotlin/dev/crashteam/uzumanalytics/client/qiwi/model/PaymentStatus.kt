package dev.crashteam.uzumanalytics.client.qiwi.model

import java.time.OffsetDateTime

data class PaymentStatus(
    val value: String,
    val changedDateTime: OffsetDateTime
)
