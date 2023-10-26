package dev.crashteam.uzumanalytics.repository.clickhouse

import dev.crashteam.uzumanalytics.repository.clickhouse.mapper.*
import dev.crashteam.uzumanalytics.repository.clickhouse.model.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.stereotype.Repository
import ru.yandex.clickhouse.ClickHouseArray
import ru.yandex.clickhouse.domain.ClickHouseDataType
import java.sql.PreparedStatement
import java.sql.Types
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.function.BinaryOperator
import java.util.stream.Collectors

@Repository
class CHProductRepository(
    @Qualifier("clickHouseJdbcTemplate") private val jdbcTemplate: JdbcTemplate
) {

    private companion object {
        private const val GET_PRODUCT_HISTORY_SQL = """
            SELECT date,
                   product_id,
                   sku_id,
                   title,
                   if(available_amount_diff < 0 OR available_amount_diff > 0 AND total_orders_amount_diff = 0,
                      total_orders_amount_diff, available_amount_diff)                  AS order_amount,
                   reviews_amount_diff                                                  AS review_amount,
                   full_price / 100 AS full_price,
                   purchase_price / 100 AS purchase_price,
                   photo_key,
                   if(available_amount_diff < 0 OR available_amount_diff > 0 AND total_orders_amount_diff = 0,
                      total_orders_amount_diff, available_amount_diff) * purchase_price / 100 AS sales_amount,
                   if(available_amount_diff < 0, 0, available_amount_diff) AS available_amount,
                   total_available_amount
            FROM (
                 SELECT date,
                        product_id,
                        sku_id,
                        title,
                        available_amount_max - available_amount_min       AS available_amount_diff,
                        total_orders_amount_max - total_orders_amount_min AS total_orders_amount_diff,
                        total_available_amount,
                        reviews_amount_max - reviews_amount_min           AS reviews_amount_diff,
                        purchase_price,
                        full_price,
                        photo_key
                 FROM (
                          SELECT date,
                                 product_id,
                                 sku_id,
                                 any(title)               AS title,
                                 min(available_amount)    AS available_amount_min,
                                 max(available_amount)    AS available_amount_max,
                                 min(total_orders_amount) AS total_orders_amount_min,
                                 max(total_orders_amount) AS total_orders_amount_max,
                                 max(total_available_amount) AS total_available_amount,
                                 min(reviews_amount)      AS reviews_amount_min,
                                 max(reviews_amount)      AS reviews_amount_max,
                                 any(purchase_price)      AS purchase_price,
                                 any(full_price)          AS full_price,
                                 any(photo_key)           AS photo_key
                          FROM uzum.product
                          WHERE product_id = ?
                            AND sku_id = ?
                            AND timestamp BETWEEN ? AND ?
                          GROUP BY product_id, sku_id, toDate(timestamp) AS date
                          ORDER BY date
                 )
         )
        """
        private val GET_PRODUCTS_SALES = """
            WITH product_sales AS
         (SELECT product_id,
                 title,
                 if(available_amount_diff < 0 OR available_amount_diff > 0 AND total_orders_amount_diff = 0,
                    total_orders_amount_diff, available_amount_diff)                  AS order_amount,
                 purchase_price / 100,
                 if(available_amount_diff < 0 OR available_amount_diff > 0 AND total_orders_amount_diff = 0,
                    total_orders_amount_diff, available_amount_diff) * purchase_price / 100 AS sales_amount,
                 seller_title,
                 seller_link,
                 seller_account_id
          FROM (
                   SELECT product_id,
                          sku_id,
                          title,
                          available_amount_max - available_amount_min       AS available_amount_diff,
                          total_orders_amount_max - total_orders_amount_min AS total_orders_amount_diff,
                          purchase_price,
                          seller_title,
                          seller_link,
                          seller_account_id
                   FROM (
                            SELECT product_id,
                                   sku_id,
                                   any(title)               AS title,
                                   min(available_amount)    AS available_amount_min,
                                   max(available_amount)    AS available_amount_max,
                                   min(total_orders_amount) AS total_orders_amount_min,
                                   max(total_orders_amount) AS total_orders_amount_max,
                                   any(purchase_price)      AS purchase_price,
                                   max(seller_title)        AS seller_title,
                                   max(seller_link)         AS seller_link,
                                   max(seller_account_id)   AS seller_account_id
                            FROM uzum.product
                            WHERE product_id IN (?)
                              AND timestamp BETWEEN ? AND ?
                            GROUP BY product_id, sku_id, toDate(timestamp) AS date
                            ORDER BY date
                            )
                   ))
          SELECT s.product_id,
                 any(s.title)             AS title,
                 sum(s.order_amount)      AS order_amount,
                 sum(s.sales_amount)      AS sales_amount,
                 any(s.seller_title)      AS seller_title,
                 any(s.seller_link)       AS seller_link,
                 any(s.seller_account_id) AS seller_account_id,
                 avg(s.order_amount)      AS daily_order_amount
          FROM product_sales s
          GROUP BY product_id
        """.trimIndent()
        private val GET_CATEGORY_OVERALL_INFO = """
            WITH category_products AS (SELECT p.timestamp,
                                  p.product_id,
                                  p.sku_id,
                                  p.latest_category_id,
                                  p.total_orders_amount,
                                  p.purchase_price,
                                  p.seller_id
                           FROM uzum.product p
                           WHERE timestamp BETWEEN ? AND ?
                             AND latest_category_id IN
                                 if(length(dictGetDescendants('categories_hierarchical_dictionary', ?, 0)) >
                                    0,
                                    dictGetDescendants('categories_hierarchical_dictionary', ?, 0),
                                    array(?)))

            SELECT round((sum(price) / 100) / count(), 2)     AS avg_price,
                   sum(revenue) / 100                         AS revenue,
                   sum(order_amount)                          AS order_count,
                   any((psc).1)                               AS seller_counts,
                   any((psc).2)                               AS product_counts,
                   round(sum(order_amount) / any((psc).2), 3) AS sales_per_seller,
                   any(seller_with_zero_sales_count),
                   any(product_zero_sales_count)
            FROM (WITH order_tbl AS
                           (SELECT product_id,
                                   total_orders_amount_max - total_orders_amount_min AS order_amount,
                                   seller_identifier                                 AS seller_id,
                                   purchase_price
                            FROM (SELECT product_id,
                                         min(total_orders_amount) AS total_orders_amount_min,
                                         max(total_orders_amount) AS total_orders_amount_max,
                                         max(purchase_price)      AS purchase_price,
                                         max(seller_id)           AS seller_identifier
                                  FROM category_products
                                  GROUP BY product_id))
            
                  SELECT product_id,
                         order_amount                                                                                       AS order_amount,
                         (order_amount) * purchase_price                                                                    AS revenue,
                         purchase_price                                                                                     AS price,
                         seller_id,
                         (SELECT uniq(seller_id) AS seller_count, uniq(product_id) AS product_count FROM category_products) AS psc,
                         (SELECT count()
                          FROM (
                                   SELECT sum(order_amount) AS order_amount
                                   FROM order_tbl
                                   GROUP BY seller_id
                                   )
                          WHERE order_amount <= 0)                                                                          AS seller_with_zero_sales_count,
                         (SELECT count() FROM order_tbl WHERE order_amount <= 0)                                            AS product_zero_sales_count
                  FROM order_tbl)
            WHERE order_amount > 0;
        """.trimIndent()
        private val GET_SELLER_OVERALL_INFO = """
            WITH product_sales AS
                (SELECT date,
                        product_id,
                        sku_id,
                        title,
                        if(available_amount_diff < 0 OR available_amount_diff > 0 AND total_orders_amount_diff = 0,
                           total_orders_amount_diff, available_amount_diff)                  AS order_amount,
                        if(available_amount_diff < 0 OR available_amount_diff > 0 AND total_orders_amount_diff = 0,
                           total_orders_amount_diff, available_amount_diff) * purchase_price AS revenue,
                        purchase_price,
                        if(available_amount_diff < 0 OR available_amount_diff > 0 AND total_orders_amount_diff = 0,
                           total_orders_amount_diff, available_amount_diff) * purchase_price AS sales_amount,
                        available_amount
                 FROM (
                          SELECT date,
                                 product_id,
                                 sku_id,
                                 title,
                                 available_amount_max                              AS available_amount,
                                 available_amount_max - available_amount_min       AS available_amount_diff,
                                 total_orders_amount_max - total_orders_amount_min AS total_orders_amount_diff,
                                 purchase_price
                          FROM (
                                   SELECT date,
                                          product_id,
                                          sku_id,
                                          any(title)               AS title,
                                          min(available_amount)    AS available_amount_min,
                                          max(available_amount)    AS available_amount_max,
                                          min(total_orders_amount) AS total_orders_amount_min,
                                          max(total_orders_amount) AS total_orders_amount_max,
                                          any(purchase_price)      AS purchase_price
                                   FROM uzum.product
                                   WHERE seller_link = ?
                                     AND timestamp BETWEEN ? AND ?
                                   GROUP BY product_id, sku_id, toDate(timestamp) AS date
                                   ORDER BY date
                                   )
                          ))

            SELECT sum(order_amount_sum)                                         AS order_amount,
                   sum(revenue) / 100                                            AS revenue,
                   count(product_id)                                             AS product_count,
                   countIf(order_amount_sum > 0)                                 AS product_with_sales,
                   round((sum(avg_price) / 100) / countIf(order_amount_sum > 0)) AS avg_price,
                   countIf(order_amount_sum <= 0)                                AS product_without_sales
            FROM (
                     SELECT product_id,
                            any(title)               AS title,
                            sum(order_amount)        AS order_amount_sum,
                            sum(revenue)             AS revenue,
                            max(available_amount)    AS last_available_amount,
                            avg(purchase_price)      AS avg_price
                     FROM product_sales s
                     GROUP BY product_id
                     )
        """.trimIndent()
        private val GET_SELLER_ORDER_DYNAMIC = """
            WITH product_sales AS
                (SELECT date,
                        product_id,
                        sku_id,
                        title,
                        if(available_amount_diff < 0 OR available_amount_diff > 0 AND total_orders_amount_diff = 0,
                           total_orders_amount_diff, available_amount_diff)                  AS order_amount,
                        if(available_amount_diff < 0 OR available_amount_diff > 0 AND total_orders_amount_diff = 0,
                           total_orders_amount_diff, available_amount_diff) * purchase_price AS revenue,
                        purchase_price,
                        if(available_amount_diff < 0 OR available_amount_diff > 0 AND total_orders_amount_diff = 0,
                           total_orders_amount_diff, available_amount_diff) * purchase_price AS sales_amount,
                        available_amount
                 FROM (
                          SELECT date,
                                 product_id,
                                 sku_id,
                                 title,
                                 available_amount_max                              AS available_amount,
                                 available_amount_max - available_amount_min       AS available_amount_diff,
                                 total_orders_amount_max - total_orders_amount_min AS total_orders_amount_diff,
                                 purchase_price
                          FROM (
                                   SELECT date,
                                          product_id,
                                          sku_id,
                                          any(title)               AS title,
                                          min(available_amount)    AS available_amount_min,
                                          max(available_amount)    AS available_amount_max,
                                          min(total_orders_amount) AS total_orders_amount_min,
                                          max(total_orders_amount) AS total_orders_amount_max,
                                          any(purchase_price)      AS purchase_price
                                   FROM uzum.product
                                   WHERE seller_link = ?
                                     AND timestamp BETWEEN ? AND ?
                                   GROUP BY product_id, sku_id, toDate(timestamp) AS date
                                   ORDER BY date
                                   )
                          ))

            SELECT date, sum(order_amount) AS order_amount FROM product_sales GROUP BY date
        """.trimIndent()
    }


    fun saveProducts(productFetchList: List<ChUzumProduct>) {
        jdbcTemplate.batchUpdate(
            "INSERT INTO uzum.product " +
                    "(timestamp, product_id, sku_id, title, rating, latest_category_id, reviews_amount," +
                    " total_orders_amount, total_available_amount, available_amount, attributes," +
                    " tags, photo_key, characteristics, seller_id, seller_account_id, seller_title, seller_link," +
                    " seller_registrationDate, seller_rating, seller_reviewsCount, seller_orders, seller_contacts, " +
                    " is_eco, is_adult, full_price, purchase_price)" +
                    " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            ProductBatchPreparedStatementSetter(productFetchList)
        )
    }

    fun getProductSales(
        productId: String,
        skuId: String,
        fromTime: LocalDateTime,
        toTime: LocalDateTime,
    ): List<ChProductSalesHistory> {
        return jdbcTemplate.query(
            GET_PRODUCT_HISTORY_SQL,
            ProductHistoryStatementSetter(productId, skuId, fromTime, toTime),
            ProductSalesHistoryMapper()
        )
    }

    fun getProductsSales(
        productIds: List<String>,
        fromTime: LocalDateTime,
        toTime: LocalDateTime,
    ): List<ChProductsSales> {
        return jdbcTemplate.query(
            GET_PRODUCTS_SALES,
            ProductsSalesStatementSetter(productIds, fromTime, toTime),
            ProductsSalesMapper()
        )
    }

    fun getCategoryAnalytics(
        categoryId: Long,
        fromTime: LocalDateTime,
        toTime: LocalDateTime,
    ): ChCategoryOverallInfo? {
        return jdbcTemplate.queryForObject(
            GET_CATEGORY_OVERALL_INFO,
            CategoryOverallInfoMapper(),
            fromTime, toTime, categoryId, categoryId, categoryId
        )
    }

    fun getSellerAnalytics(
        sellerLink: String,
        fromTime: LocalDateTime,
        toTime: LocalDateTime,
    ): ChSellerOverallInfo? {
        return jdbcTemplate.queryForObject(
            GET_SELLER_OVERALL_INFO,
            SellerOverallInfoMapper(),
            sellerLink, fromTime, toTime
        )
    }

    fun getSellerOrderDynamic(
        sellerLink: String,
        fromTime: LocalDateTime,
        toTime: LocalDateTime,
    ): List<ChSellerOrderDynamic> {
        return jdbcTemplate.query(
            GET_SELLER_ORDER_DYNAMIC,
            SellerOrderDynamicMapper(),
            sellerLink, fromTime, toTime
        )
    }

    internal class ProductHistoryStatementSetter(
        private val productId: String,
        private val skuId: String,
        private val fromTime: LocalDateTime,
        private val toTime: LocalDateTime,
    ) : PreparedStatementSetter {
        override fun setValues(ps: PreparedStatement) {
            var l = 1
            ps.setString(l++, productId)
            ps.setString(l++, skuId)
            ps.setObject(l++, fromTime)
            ps.setObject(l++, toTime)
        }
    }

    internal class ProductsSalesStatementSetter(
        private val productIds: List<String>,
        private val fromTime: LocalDateTime,
        private val toTime: LocalDateTime
    ) : PreparedStatementSetter {
        override fun setValues(ps: PreparedStatement) {
            var l = 1
            ps.setArray(l++, ClickHouseArray(ClickHouseDataType.String, productIds.toTypedArray()))
            ps.setObject(l++, fromTime)
            ps.setObject(l++, toTime)
        }
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
            ps.setLong(l++, product.categoryPaths.last())
            ps.setInt(l++, product.reviewsAmount)
            ps.setLong(l++, product.totalOrdersAmount)
            ps.setLong(l++, product.totalAvailableAmount)
            ps.setLong(l++, product.availableAmount)
            ps.setArray(l++, ClickHouseArray(ClickHouseDataType.String, product.attributes.toTypedArray()))
            ps.setArray(l++, ClickHouseArray(ClickHouseDataType.String, product.tags.toTypedArray()))
            ps.setString(l++, product.photoKey)
            ps.setObject(
                l++,
                product.characteristics.stream().collect(Collectors.toMap({ it.type }, { it.title }) { _, u -> u })
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
