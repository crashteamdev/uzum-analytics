package dev.crashteam.uzumanalytics.mongo

import java.math.BigDecimal

data class ProductItemChangeDocument(
    val title: String,

    val parentCategory: String?,

    val ancestorCategories: List<String>?,

    val reviewsAmount: Long,

    val orderAmount: Long,

    val rOrdersAmount: Long,

    val rating: BigDecimal?,

    val totalAvailableAmount: Long,

    val description: String,

    val attributes: List<String>,

    val tags: List<String>,

    val seller: SellerChangeDocument,

    val split: List<ProductSplitChangeDocument>?,
)

data class SellerChangeDocument(
    val id: Long,
    val title: String,
    val link: String,
    val description: String?,
    val rating: BigDecimal,
    val sellerAccountId: Long,
    val isEco: Boolean,
    val adultCategory: Boolean,
    val contacts: List<ProductContactChangeDocument>,
)

data class ProductSplitChangeDocument(
    val id: Long,
    val characteristics: List<ProductSplitCharacteristicChangeDocument>,
    val availableAmount: Long,
    val fullPrice: BigDecimal?,
    val purchasePrice: BigDecimal,
    val photoKey: String?
)

data class ProductSplitCharacteristicChangeDocument(
    val type: String,
    val title: String,
    val value: String,
)

data class ProductContactChangeDocument(
    val type: String,
    val value: String,
)
