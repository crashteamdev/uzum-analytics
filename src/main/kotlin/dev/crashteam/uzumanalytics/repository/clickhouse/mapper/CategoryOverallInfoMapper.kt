package dev.crashteam.uzumanalytics.repository.clickhouse.mapper

import dev.crashteam.uzumanalytics.repository.clickhouse.model.ChCategoryOverallInfo
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

class CategoryOverallInfoMapper : RowMapper<ChCategoryOverallInfo> {

    override fun mapRow(rs: ResultSet, rowNum: Int): ChCategoryOverallInfo {
//        val zeroSalesArray = rs.getObject("zero_sales", String::class.java)
//            .removePrefix("(").removeSuffix(")")
//            .split(",")
        return ChCategoryOverallInfo(
            averagePrice = rs.getBigDecimal("avg_price"),
            revenue = rs.getBigDecimal("revenue"),
            orderCount = rs.getLong("order_count"),
            sellerCount = rs.getLong("seller_counts"),
            salesPerSeller = rs.getBigDecimal("sales_per_seller"),
            productCount = rs.getLong("product_counts"),
            productZeroSalesCount = rs.getLong("product_zero_sales_count"),
            sellersZeroSalesCount = rs.getLong("seller_with_zero_sales_count"),
        )
    }
}
