package dev.crashteam.uzumanalytics.converter.clickhouse

import dev.crashteam.uzum.scrapper.data.v1.UzumProductChange
import dev.crashteam.uzumanalytics.converter.DataConverter
import dev.crashteam.uzumanalytics.extension.toLocalDateTime
import dev.crashteam.uzumanalytics.repository.clickhouse.model.ChKazanExpressCharacteristic
import dev.crashteam.uzumanalytics.repository.clickhouse.model.ChUzumProduct
import dev.crashteam.uzumanalytics.stream.handler.model.UzumProductWrapper
import dev.crashteam.uzumanalytics.stream.model.UzumProductCategoryStreamRecord
import org.springframework.stereotype.Component
import java.util.stream.Collectors

@Component
class UzumProductItemToChProductConverter :
    DataConverter<UzumProductWrapper, ChUzumProductConverterResultWrapper> {

    override fun convert(source: UzumProductWrapper): ChUzumProductConverterResultWrapper {
        val uzumProductChange = source.product
        return ChUzumProductConverterResultWrapper(uzumProductChange.skusList.map { sku ->
            ChUzumProduct(
                fetchTime = source.eventTime.toLocalDateTime(),
                productId = uzumProductChange.productId.toLong(),
                skuId = sku.skuId.toLong(),
                title = uzumProductChange.title,
                categoryPaths = categoryToPath(uzumProductChange.category),
                rating = uzumProductChange.rating.toBigDecimal(),
                reviewsAmount = uzumProductChange.reviewsAmount.toInt(),
                totalOrdersAmount = uzumProductChange.orders,
                totalAvailableAmount = uzumProductChange.totalAvailableAmount,
                availableAmount = sku.availableAmount,
                fullPrice = sku.fullPrice?.toBigDecimal()?.movePointRight(2)?.toLong(),
                purchasePrice = sku.purchasePrice.toBigDecimal().movePointRight(2).toLong(),
                attributes = uzumProductChange.attributesList,
                tags = uzumProductChange.tagsList,
                photoKey = sku.photoKey,
                characteristics = sku.characteristicsList.map {
                    ChKazanExpressCharacteristic(it.type, it.title)
                },
                sellerId = uzumProductChange.seller.id,
                sellerAccountId = uzumProductChange.seller.accountId,
                sellerTitle = uzumProductChange.seller.sellerTitle,
                sellerLink = uzumProductChange.seller.sellerLink,
                sellerRegistrationDate = uzumProductChange.seller.registrationDate.seconds,
                sellerRating = uzumProductChange.seller.rating.toBigDecimal(),
                sellerReviewsCount = uzumProductChange.seller.reviews.toInt(),
                sellerOrders = uzumProductChange.seller.orders,
                sellerContacts = uzumProductChange.seller.contactsList.stream()
                    .collect(Collectors.toMap({ it.type }, { it.value })),
                isEco = uzumProductChange.isEco,
                adultCategory = uzumProductChange.isAdult,
            )
        })
    }

    private fun categoryToPath(category: UzumProductChange.UzumProductCategory): List<Long> {
        val paths = mutableListOf<Long>()
        var nextCategory: UzumProductChange.UzumProductCategory? = category
        while (nextCategory != null) {
            paths.add(category.id)
            nextCategory = nextCategory.parent
        }
        return paths.toList()
    }
}
