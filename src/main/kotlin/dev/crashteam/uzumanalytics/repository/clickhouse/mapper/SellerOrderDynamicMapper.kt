package dev.crashteam.uzumanalytics.repository.clickhouse.mapper

import dev.crashteam.uzumanalytics.repository.clickhouse.model.ChSellerOrderDynamic
import dev.crashteam.uzumanalytics.repository.clickhouse.model.ChSellerOverallInfo
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

class SellerOrderDynamicMapper : RowMapper<ChSellerOrderDynamic> {
    override fun mapRow(rs: ResultSet, rowNum: Int): ChSellerOrderDynamic {
        return ChSellerOrderDynamic(
            date = rs.getDate("date").toLocalDate(),
            orderAmount = rs.getLong("order_amount")
        )
    }
}
