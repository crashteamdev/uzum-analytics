package dev.crashteam.uzumanalytics.service.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

class ProductSkuHistorical(
    val productId: Long,
    val skuId: Long,
    val name: String,
    val orderAmount: Long,
    val reviewsAmount: Long,
    val totalAvailableAmount: Long,
) {
    var fullPrice: BigDecimal? = null
    var price: BigDecimal? = null
    var availableAmount: Long? = null
    var salesAmount: BigDecimal? = null
    var photoKey: String? = null
    var characteristic: List<ProductSkuHistoricalCharacteristic> = emptyList()
    var date: Instant? = null
}

data class ProductSkuHistoricalCharacteristic(
    val type: String,
    val title: String,
    val value: String
)
