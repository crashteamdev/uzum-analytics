package dev.crashteam.uzumanalytics.repository.clickhouse.mapper

import dev.crashteam.uzumanalytics.repository.clickhouse.model.ChCategoryHierarchy
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

class CategoryHierarchyMapper : RowMapper<ChCategoryHierarchy> {

    override fun mapRow(rs: ResultSet, rowNum: Int): ChCategoryHierarchy {
        return ChCategoryHierarchy(
            name = rs.getString("name"),
            parentId = rs.getLong("parent_id"),
            childrenIds = (rs.getArray("children_ids").array as LongArray).toList(),
        )
    }
}
