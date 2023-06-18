package dev.crashteam.uzumanalytics.converter.view

import dev.crashteam.uzumanalytics.controller.model.ProductSellerView
import dev.crashteam.uzumanalytics.converter.DataConverter
import dev.crashteam.uzumanalytics.domain.mongo.SellerDocument
import org.springframework.stereotype.Component

@Component
class ProductSellerDocumentToDataConverter : DataConverter<SellerDocument, ProductSellerView> {

    override fun convert(source: SellerDocument): ProductSellerView {
        return ProductSellerView(
            id = source.id,
            title = source.title,
            link = source.link,
            description = source.description,
            rating = source.rating,
            sellerAccountId = source.sellerAccountId,
            isEco = source.isEco ?: false,
            adultCategory = source.adultCategory ?: false,
        )
    }
}
