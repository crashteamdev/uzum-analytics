package dev.crashteam.uzumanalytics.converter.view

import dev.crashteam.uzumanalytics.controller.model.ProductItemView
import dev.crashteam.uzumanalytics.controller.model.ProductSellerView
import dev.crashteam.uzumanalytics.controller.model.ProductView
import dev.crashteam.uzumanalytics.converter.DataConverter
import dev.crashteam.uzumanalytics.domain.mongo.ProductDocument
import org.springframework.context.annotation.Lazy
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

@Component
class ProductDocumentToDataConverter(
    @Lazy private val conversionService: ConversionService
) : DataConverter<ProductDocument, ProductView> {

    override fun convert(source: ProductDocument): ProductView? {
        return ProductView(
            productId = source.productId,
            title = source.title,
            parentCategory = source.parentCategory,
            ancestorCategories = source.ancestorCategories,
            reviewsAmount = source.reviewsAmount,
            orderAmount = source.orderAmount,
            rOrdersAmount = source.rOrdersAmount ?: source.orderAmount,
            totalAvailableAmount = source.totalAvailableAmount,
            description = source.description,
            attributes = source.attributes,
            tags = source.tags,
            seller = conversionService.convert(source.seller, ProductSellerView::class.java)!!,
            items = source.split?.map { conversionService.convert(it, ProductItemView::class.java)!! },
        )
    }
}
