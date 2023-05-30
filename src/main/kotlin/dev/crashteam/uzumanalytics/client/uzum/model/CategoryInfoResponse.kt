package dev.crashteam.uzumanalytics.client.uzum.model

data class CategoryInfoResponse(
    val payload: CategoryInfo
)

data class CategoryInfo(
    val category: CategoryData
)

data class CategoryData(
    val id: Long,
    val productAmount: Long,
    val adult: Boolean,
    val eco: Boolean,
    val title: String,
    val children: List<CategoryData>
)
