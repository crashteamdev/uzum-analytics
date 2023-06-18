package dev.crashteam.uzumanalytics.stream.model

data class UzumCategoryStreamRecord(
    val id: Long,
    val adult: Boolean,
    val eco: Boolean,
    val title: String,
    val children: List<UzumCategoryStreamRecord>?,
    val time: Long,
)
