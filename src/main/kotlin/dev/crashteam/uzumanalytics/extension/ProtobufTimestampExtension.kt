package dev.crashteam.uzumanalytics.extension

import com.google.protobuf.Timestamp
import java.time.LocalDateTime
import java.time.ZoneOffset

fun Timestamp.toLocalDateTime(): LocalDateTime {
    return LocalDateTime.ofEpochSecond(
        this.seconds,
        this.nanos,
        ZoneOffset.UTC,
    )
}
