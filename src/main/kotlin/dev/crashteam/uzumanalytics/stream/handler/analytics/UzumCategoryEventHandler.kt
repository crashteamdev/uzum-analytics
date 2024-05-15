package dev.crashteam.uzumanalytics.stream.handler.analytics

import dev.crashteam.uzum.scrapper.data.v1.UzumCategory
import dev.crashteam.uzum.scrapper.data.v1.UzumScrapperEvent
import dev.crashteam.uzumanalytics.db.model.tables.pojos.CategoryHierarchical
import dev.crashteam.uzumanalytics.repository.postgres.CategoryRepository
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class UzumCategoryEventHandler(
    private val categoryRepository: CategoryRepository,
) : UzumScrapEventHandler {

    override fun handle(events: List<UzumScrapperEvent>) {
        runBlocking {
            val uzumCategoryChanges = events.map { it.eventPayload.uzumCategoryChange }
            try {
                for (uzumCategoryChange in uzumCategoryChanges) {
                    log.info {
                        "Consume category from stream." +
                                " categoryId=${uzumCategoryChange.category.categoryId};" +
                                " childCount=${uzumCategoryChange.category.childrenList?.size}"
                    }
                    saveHierarchicalRootCategory(uzumCategoryChange.category)
                }
            } catch (e: Exception) {
                log.error(e) { "Exception during handle category message" }
            }
        }
    }

    override fun isHandle(event: UzumScrapperEvent): Boolean {
        return event.eventPayload.hasUzumCategoryChange()
    }

    private suspend fun saveHierarchicalRootCategory(
        rootCategoryRecord: UzumCategory,
    ) {
        val rootCategory = CategoryHierarchical(
            rootCategoryRecord.categoryId,
            0,
            rootCategoryRecord.title
        )
        log.info { "Save root category: $rootCategory" }
        categoryRepository.save(rootCategory)
        if (rootCategoryRecord.childrenList?.isNotEmpty() == true) {
            saveHierarchicalChildCategory(rootCategoryRecord, rootCategoryRecord.childrenList)
        }
    }

    private suspend fun saveHierarchicalChildCategory(
        currentCategoryRecord: UzumCategory,
        childCategoryRecords: List<UzumCategory>
    ) {
        for (childrenRecord in childCategoryRecords) {
            val childCategory = CategoryHierarchical(
                childrenRecord.categoryId,
                currentCategoryRecord.categoryId,
                childrenRecord.title
            )
            categoryRepository.save(childCategory)
            if (childrenRecord.childrenList?.isNotEmpty() == true) {
                saveHierarchicalChildCategory(childrenRecord, childrenRecord.childrenList)
            }
        }
    }
}
