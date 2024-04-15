package dev.crashteam.uzumanalytics.converter.clickhouse

import dev.crashteam.mp.external.analytics.category.ProductCategory
import dev.crashteam.mp.external.analytics.category.ProductSeller
import dev.crashteam.uzumanalytics.converter.DataConverter
import dev.crashteam.uzumanalytics.extensions.toMoney
import dev.crashteam.uzumanalytics.extensions.toProtobufDate
import org.springframework.stereotype.Component
import java.math.RoundingMode

@Component
class ProductDailyAnalyticsToProtoConverter :
    DataConverter<dev.crashteam.uzumanalytics.service.model.ProductDailyAnalytics, dev.crashteam.mp.external.analytics.category.ProductDailyAnalytics> {
    override fun convert(source: dev.crashteam.uzumanalytics.service.model.ProductDailyAnalytics): dev.crashteam.mp.external.analytics.category.ProductDailyAnalytics? {
        return dev.crashteam.mp.external.analytics.category.ProductDailyAnalytics.newBuilder().apply {
            this.productId = source.productId
            this.name = source.title
            this.category = ProductCategory.newBuilder().apply {
                this.categoryId = source.category.categoryId.toString()
                this.categoryName = source.category.categoryName
            }.build()
            this.seller = ProductSeller.newBuilder().apply {
                this.sellerLink = source.seller.sellerLink
                this.sellerTitle = source.seller.sellerTitle
            }.build()
            this.price = source.price.toMoney()
            this.fullPrice = source.fullPrice.toMoney()
            this.revenue = source.revenue.toMoney()
            this.appearAt = source.firstDiscovered.toLocalDate().toProtobufDate()
            this.reviewsCount = source.reviewAmount
            this.rating = source.rating.setScale(1, RoundingMode.HALF_UP).toDouble()
            this.addAllPriceChart(source.priceChart.map { it.toMoney() })
            this.addAllSalesChart(source.orderChart)
            this.addAllRevenueChart(source.revenueChart.map { it.toMoney() })
            this.addAllAvailableCountChart(source.availableChart)
        }.build()
    }
}
