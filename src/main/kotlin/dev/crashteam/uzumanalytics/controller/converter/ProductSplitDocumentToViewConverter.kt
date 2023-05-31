package dev.crashteam.uzumanalytics.controller.converter

import dev.crashteam.uzumanalytics.controller.model.ItemCharacteristicView
import dev.crashteam.uzumanalytics.controller.model.ProductItemView
import dev.crashteam.uzumanalytics.mongo.ProductSplitDocument
import org.springframework.context.annotation.Lazy
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

@Component
class ProductSplitDocumentToViewConverter(
    @Lazy private val conversionService: ConversionService,
) : ViewConverter<ProductSplitDocument, ProductItemView> {

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
