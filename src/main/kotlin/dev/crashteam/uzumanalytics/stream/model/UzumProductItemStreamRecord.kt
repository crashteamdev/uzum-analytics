package dev.crashteam.uzumanalytics.stream.model

data class UzumProductItemStreamRecord(
    val productId: Long,
    val time: Long,
    val title: String,
    val totalAvailableAmount: Long,
    val orders: Long,
    val description: String?,
    val attributes: List<String>,
    val tags: List<String>,
    val reviewsAmount: Long,
    val rating: String,
    val skuList: List<UzumItemSkuStreamRecord>,
    val seller: UzumProductSellerStreamRecord,
    val category: UzumProductCategoryStreamRecord,
    val isEco: Boolean,
    val isAdult: Boolean,
    val characteristics: List<UzumProductCharacteristicsStreamRecord>
)

data class UzumProductCharacteristicsStreamRecord(
    val id: Long,
    val title: String,
    val values: List<UzumProductCharacteristicStreamRecord>
)

data class UzumProductCharacteristicStreamRecord(
    val id: Long,
    val title: String,
    val value: String
)

data class UzumProductCategoryStreamRecord(
    val id: Long,
    val title: String,
    val productAmount: Long,
    val parent: UzumProductCategoryStreamRecord?
)

data class UzumItemSkuStreamRecord(
    val skuId: Long,
    val photoKey: String,
    val characteristics: List<UzumItemCharacteristicStreamRecord>,
    val purchasePrice: String,
    val fullPrice: String?,
    val availableAmount: Long,
)

data class UzumItemCharacteristicStreamRecord(
    val type: String,
    val title: String,
    val value: String,
)

data class UzumProductSellerStreamRecord(
    val id: Long,
    val accountId: Long,
    val sellerLink: String,
    val sellerTitle: String,
    val reviews: Long,
    val orders: Long,
    val rating: String,
    val registrationDate: Long,
    val description: String?,
    val contacts: List<UzumProductSellerContact>,
)

data class UzumProductSellerContact(
    val type: String,
    val value: String,
)
