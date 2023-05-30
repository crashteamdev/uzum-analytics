package dev.crashteam.uzumanalytics.client.uzum.model

data class UzumGQLQueryResponse<T>(
    val data: UzumGQLQueryResponseData<T>?,
    val errors: List<GQLError>?
)

data class UzumGQLQueryResponseData<T>(
    val makeSearch: T
)

data class ShopGQLQueryResponse(
    val items: List<ShopGQLCatalogCard>
)

data class ShopGQLCatalogCard(
    val catalogCard: ShopSkuGroupCard
)

data class ShopSkuGroupCard(
    val id: Long,
    val productId: Long,
    val title: String,
    val adult: Boolean,
    val characteristicValues: List<SkuGQLCharacteristicValue>?,
    val feedbackQuantity: Long,
    val minFullPrice: Long,
    val minSellPrice: Long,
    val ordersQuantity: Long
)

data class SkuGQLCharacteristicValue(
    val id: Long,
    val title: String,
    val value: String
)
