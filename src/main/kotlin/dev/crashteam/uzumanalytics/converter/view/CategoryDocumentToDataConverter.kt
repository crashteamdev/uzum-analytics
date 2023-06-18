package dev.crashteam.uzumanalytics.converter.view

import dev.crashteam.uzumanalytics.controller.model.CategoryView
import dev.crashteam.uzumanalytics.converter.DataConverter
import dev.crashteam.uzumanalytics.domain.mongo.CategoryDocument
import org.springframework.stereotype.Component

@Component
class CategoryDocumentToDataConverter : DataConverter<CategoryDocument, CategoryView> {

    override fun convert(category: CategoryDocument): CategoryView {
        return convertToView(category)
    }

    private fun convertToView(category: CategoryDocument): CategoryView {
        return CategoryView().apply {
            categoryId = category.publicId
            title = category.title
            adult = category.adult
            eco = category.eco
            productAmount = category.productAmount
            child = category.document.map { convertToView(it) }
        }
    }
}
