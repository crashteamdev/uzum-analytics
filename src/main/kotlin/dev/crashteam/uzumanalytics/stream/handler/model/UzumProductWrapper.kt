package dev.crashteam.uzumanalytics.stream.handler.model

import com.google.protobuf.Timestamp
import dev.crashteam.uzum.scrapper.data.v1.UzumProductChange

data class UzumProductWrapper(
    val product: UzumProductChange,
    val eventTime: Timestamp,
)
