package dev.crashteam.uzumanalytics.report.model

import java.io.InputStream

data class Report(
    val name: String,
    val stream: InputStream
)
