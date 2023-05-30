package dev.crashteam.uzumanalytics.client.uzum.model

import java.math.BigDecimal

data class CategoryResponseV2(
    val payload: CategoryPayloadV2?,
    val error: String?
)

data class CategoryPayloadV2(
    val products: List<CategoryProduct>,
    val adultContent: Boolean,
    val totalProducts: Int
)

data class CategoryProduct(
    val productId: Long,
    val title: String,
    val sellPrice: BigDecimal,
    val fullPrice: BigDecimal?,
    val compressedImage: String,
    val image: String,
    val rating: BigDecimal,
    val ordersQuantity: Long,
    val rOrdersQuantity: Long,
)
