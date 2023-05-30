package dev.crashteam.uzumanalytics.domain.mongo.converter

import dev.crashteam.uzumanalytics.client.uzum.model.Seller
import dev.crashteam.uzumanalytics.controller.converter.ViewConverter
import dev.crashteam.uzumanalytics.domain.mongo.*
import org.springframework.stereotype.Component

@Component
class SellerMongoDocumentConverter : ViewConverter<Seller, SellerDocument> {

    override fun convert(source: Seller): SellerDocument {
        return SellerDocument(
            id = source.id,
            title = source.title,
            link = source.link,
            description = source.description,
            rating = source.rating,
            sellerAccountId = source.sellerAccountId,
            isEco = source.isEco,
            adultCategory = source.adultCategory,
            contacts = source.contacts.map { ProductContactDocument(it.type, it.value) }
        )
    }

}
