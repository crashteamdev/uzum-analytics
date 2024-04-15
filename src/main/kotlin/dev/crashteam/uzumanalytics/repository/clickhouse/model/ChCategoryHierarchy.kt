package dev.crashteam.uzumanalytics.repository.clickhouse.model

data class ChCategoryHierarchy(
    val name: String,
    val parentId: Long,
    val childrenIds: List<Long>,
)
