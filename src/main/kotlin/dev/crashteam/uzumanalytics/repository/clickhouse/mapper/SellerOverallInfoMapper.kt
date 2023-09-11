package dev.crashteam.uzumanalytics.repository.clickhouse.mapper

import dev.crashteam.uzumanalytics.repository.clickhouse.model.ChSellerOverallInfo
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

class SellerOverallInfoMapper : RowMapper<ChSellerOverallInfo> {
    override fun mapRow(rs: ResultSet, rowNum: Int): ChSellerOverallInfo {
        return ChSellerOverallInfo(
            averagePrice = rs.getBigDecimal("avg_price"),
            revenue = rs.getBigDecimal("revenue"),
            orderCount = rs.getLong("order_amount"),
            productCount = rs.getLong("product_count"),
            productCountWithSales = rs.getLong("product_with_sales"),
            productCountWithoutSales = rs.getLong("product_without_sales"),
        )
    }
}
