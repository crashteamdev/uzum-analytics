package dev.crashteam.uzumanalytics.service

import dev.crashteam.uzumanalytics.domain.mongo.CategoryDocument
import dev.crashteam.uzumanalytics.repository.mongo.CategoryRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository
) {

    @Cacheable("category")
    fun getAllCategories(): Flux<CategoryDocument> {
        return categoryRepository.findByPathIsNull().map { rootCategory ->
            categoryRepository.findByPathOrderByPath("^,${rootCategory.title},").collectList().map {
                buildCategory(rootCategory, it)
            }
        }.flatMapSequential { it }
    }

    private fun buildCategory(
        rootCategory: CategoryDocument,
        childCategories: MutableList<CategoryDocument>
    ): CategoryDocument {
        val map = HashMap<String, CategoryDocument>()
        for (childCategory in childCategories) {
            map[childCategory.path!!] = childCategory
        }
        for (childCategory in childCategories) {
            val path = childCategory.path!!.substring(0, childCategory.path.dropLast(1).lastIndexOf(",")) + ","
            if (path.replace(",", "") == rootCategory.title) {
                rootCategory.document.add(childCategory)
            } else {
                val parent = map[path]
                parent?.document?.add(childCategory)
            }
        }
        return rootCategory
    }
}
