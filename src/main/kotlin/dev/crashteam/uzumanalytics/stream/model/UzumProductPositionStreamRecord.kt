package dev.crashteam.uzumanalytics.stream.model

data class UzumProductPositionStreamRecord(
    val position: Long,
    val productId: Long,
    val skuId: Long,
    val categoryId: Long,
    val time: Long
)
