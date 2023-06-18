package dev.crashteam.uzumanalytics.controller.converter

import dev.crashteam.uzumanalytics.controller.model.ItemCharacteristicView
import dev.crashteam.uzumanalytics.controller.model.ProductItemView
import dev.crashteam.uzumanalytics.domain.mongo.ProductSplitDocument
import org.springframework.context.annotation.Lazy
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

@Component
class ProductSplitDocumentToDataConverter(
    @Lazy private val conversionService: ConversionService,
) : DataConverter<ProductSplitDocument, ProductItemView> {

    override fun convert(source: ProductSplitDocument): ProductItemView {
        return ProductItemView(
            id = source.id,
            characteristics = source.characteristics.map {
                conversionService.convert(
                    it,
                    ItemCharacteristicView::class.java
                )!!
            },
            availableAmount = source.availableAmount,
            fullPrice = source.fullPrice,
            purchasePrice = source.purchasePrice,
            photoKey = source.photoKey
        )
    }
}
