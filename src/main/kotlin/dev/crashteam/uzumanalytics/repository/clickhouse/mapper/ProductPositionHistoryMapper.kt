package dev.crashteam.uzumanalytics.repository.clickhouse.mapper

import dev.crashteam.uzumanalytics.repository.clickhouse.model.ChProductPositionHistory
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

class ProductPositionHistoryMapper : RowMapper<ChProductPositionHistory> {
    override fun mapRow(rs: ResultSet, rowNum: Int): ChProductPositionHistory {
        return ChProductPositionHistory(
            date = rs.getDate("date").toLocalDate(),
            position = rs.getLong("position")
        )
    }
}
