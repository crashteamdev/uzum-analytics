package dev.crashteam.uzumanalytics.repository.clickhouse

import dev.crashteam.uzumanalytics.repository.clickhouse.model.ChUzumProduct
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import ru.yandex.clickhouse.ClickHouseArray
import ru.yandex.clickhouse.domain.ClickHouseDataType
import java.sql.PreparedStatement
import java.sql.Types
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.stream.Collectors

@Repository
class CHProductRepository(
    @Qualifier("clickHouseJdbcTemplate") private val jdbcTemplate: JdbcTemplate
) {

    fun saveProducts(productFetchList: List<ChUzumProduct>) {
        jdbcTemplate.batchUpdate(
            "INSERT INTO uzum.product " +
                    "(timestamp, fetchTime, productId, skuId, title, rating, categoryPath, reviewsAmount," +
                    " totalOrdersAmount, totalAvailableAmount, availableAmount, attributes," +
                    " tags, photoKey, characteristics, sellerId, sellerAccountId, sellerTitle, sellerLink," +
                    " sellerRegistrationDate, sellerRating, sellerReviewsCount, sellerOrders, sellerOfficial, sellerContacts, " +
                    " isEco, isPerishable, hasVerticalPhoto, showKitty, bonusProduct, adultCategory, colorPhotoPreview, fullPrice, " +
                    " purchasePrice, charityProfit, barcode, vatType, vatAmount, vatPrice)" +
                    " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            ProductBatchPreparedStatementSetter(productFetchList)
        )
    }

    internal class ProductBatchPreparedStatementSetter(
        private val products: List<ChUzumProduct>
    ) : BatchPreparedStatementSetter {

        override fun setValues(ps: PreparedStatement, i: Int) {
            val product: ChUzumProduct = products[i]
            var l = 1
            ps.setObject(
                l++, Instant.ofEpochSecond(
                    product.fetchTime.toEpochSecond(ZoneOffset.UTC)
                ).atZone(ZoneId.of("UTC")).toLocalDateTime()
            )
            ps.setLong(l++, product.productId)
            ps.setLong(l++, product.skuId)
            ps.setString(l++, product.title)
            ps.setObject(l++, product.rating)
            ps.setArray(
                l++,
                ClickHouseArray(ClickHouseDataType.UInt64, product.categoryPaths.toTypedArray())
            )
            ps.setInt(l++, product.reviewsAmount)
            ps.setLong(l++, product.totalOrdersAmount)
            ps.setLong(l++, product.totalAvailableAmount)
            ps.setLong(l++, product.availableAmount)
            ps.setArray(l++, ClickHouseArray(ClickHouseDataType.String, product.attributes.toTypedArray()))
            ps.setArray(l++, ClickHouseArray(ClickHouseDataType.String, product.tags.toTypedArray()))
            ps.setString(l++, product.photoKey)
            ps.setObject(
                l++,
                product.characteristics.stream().collect(Collectors.toMap({ it.type }, { it.title }))
            )
            ps.setLong(l++, product.sellerId)
            ps.setLong(l++, product.sellerAccountId)
            ps.setString(l++, product.sellerTitle)
            ps.setString(l++, product.sellerLink)
            ps.setObject(
                l++,
                Instant.ofEpochSecond(product.sellerRegistrationDate / 1000).atZone(ZoneId.of("UTC"))
                    .toLocalDateTime()
            )
            ps.setObject(l++, product.sellerRating)
            ps.setInt(l++, product.sellerReviewsCount)
            ps.setLong(l++, product.sellerOrders)
            ps.setObject(l++, product.sellerContacts)
            ps.setBoolean(l++, product.isEco)
            ps.setBoolean(l++, product.adultCategory)
            product.fullPrice?.let { ps.setLong(l++, product.fullPrice) } ?: ps.setNull(l++, Types.BIGINT)
            ps.setLong(l++, product.purchasePrice)
        }

        override fun getBatchSize(): Int {
            return products.size
        }
    }
}
