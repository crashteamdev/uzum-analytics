package dev.crashteam.uzumanalytics.converter.view

import dev.crashteam.uzumanalytics.controller.model.ItemCharacteristicView
import dev.crashteam.uzumanalytics.converter.DataConverter
import dev.crashteam.uzumanalytics.domain.mongo.ProductSplitCharacteristicDocument
import org.springframework.stereotype.Component

@Component
class ProductSplitCharacteristicDocumentToDataConverter :
    DataConverter<ProductSplitCharacteristicDocument, ItemCharacteristicView> {

    override fun convert(source: ProductSplitCharacteristicDocument): ItemCharacteristicView {
        return ItemCharacteristicView(
            type = source.type,
            title = source.title,
            value = source.value
        )
    }
}
