package dev.crashteam.uzumanalytics.service

import dev.crashteam.uzumanalytics.repository.clickhouse.CHProductRepository
import dev.crashteam.uzumanalytics.repository.clickhouse.model.ChProductSalesReport
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

@Service
class ProductServiceV2(
    private val chProductRepository: CHProductRepository,
) {

    fun getSellerSales(
        link: String,
        fromTime: LocalDateTime,
        toTime: LocalDateTime,
        limit: Int,
        offset: Int,
    ): List<ChProductSalesReport> {
        log.info { "Get seller sales by link=$link; fromTime=$fromTime; toTime=$toTime; limit=$limit; offset=$offset" }
        return chProductRepository.getSellerSalesForReport(
            sellerLink = link,
            fromTime = fromTime,
            toTime = toTime,
            limit = limit,
            offset = offset,
        )
    }

    fun getCategorySales(
        categoryId: Long,
        fromTime: LocalDateTime,
        toTime: LocalDateTime,
        limit: Int,
        offset: Int,
    ): List<ChProductSalesReport> {
        log.info { "Get category sales by categoryId=$categoryId;" +
                " fromTime=$fromTime; toTime=$toTime; limit=$limit' offset=$offset" }
        return chProductRepository.getCategorySalesForReport(
            categoryId,
            fromTime,
            toTime,
            limit,
            offset
        )
    }
}
