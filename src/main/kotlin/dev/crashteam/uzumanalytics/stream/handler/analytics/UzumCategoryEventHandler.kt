package dev.crashteam.uzumanalytics.stream.handler.analytics

import dev.crashteam.uzum.scrapper.data.v1.UzumCategory
import dev.crashteam.uzum.scrapper.data.v1.UzumScrapperEvent
import dev.crashteam.uzumanalytics.domain.mongo.CategoryDocument
import dev.crashteam.uzumanalytics.domain.mongo.CategoryTreeDocument
import dev.crashteam.uzumanalytics.repository.mongo.CategoryDao
import dev.crashteam.uzumanalytics.repository.mongo.CategoryTreeDao
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

@Component
class UzumCategoryEventHandler(
    private val categoryDao: CategoryDao,
    private val categoryTreeDao: CategoryTreeDao,
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
                    val categoryDocument = CategoryDocument(
                        uzumCategoryChange.category.categoryId,
                        null,
                        uzumCategoryChange.category.isAdult,
                        uzumCategoryChange.category.isEco,
                        uzumCategoryChange.category.title.trim(),
                        null,
                        LocalDateTime.now()
                    )
                    categoryDao.saveCategory(categoryDocument).awaitSingleOrNull()
                    saveChildCategories(
                        uzumCategoryChange.category,
                        uzumCategoryChange.category.childrenList ?: emptyList(),
                        null
                    )

                    // Save hierarchical category view
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

    private suspend fun saveChildCategories(
        rootCategory: UzumCategory,
        children: List<UzumCategory>,
        path: String?,
    ) {
        for (childCategory in children) {
            val categoryDocument = CategoryDocument(
                childCategory.categoryId,
                null,
                childCategory.isAdult,
                childCategory.isEco,
                childCategory.title.trim(),
                if (path == null) {
                    ",${rootCategory.title.trim()},${childCategory.title.trim()},"
                } else "$path${childCategory.title.trim()},",
                LocalDateTime.now()
            )
            log.info { "Save child category: $categoryDocument" }
            categoryDao.saveCategory(categoryDocument).awaitSingleOrNull()
            if (childCategory.childrenList?.isNotEmpty() == true) {
                saveChildCategories(childCategory, childCategory.childrenList, categoryDocument.path)
            }
        }
    }

    private suspend fun saveHierarchicalRootCategory(
        rootCategoryRecord: UzumCategory,
    ) {
        val rootCategory = CategoryTreeDocument(
            categoryId = rootCategoryRecord.categoryId,
            parentCategoryId = 0,
            title = rootCategoryRecord.title
        )
        log.info { "Save root category: $rootCategory" }
        categoryTreeDao.saveCategory(rootCategory).awaitSingleOrNull()
        if (rootCategoryRecord.childrenList?.isNotEmpty() == true) {
            saveHierarchicalChildCategory(rootCategoryRecord, rootCategoryRecord.childrenList)
        }
    }

    private suspend fun saveHierarchicalChildCategory(
        currentCategoryRecord: UzumCategory,
        childCategoryRecords: List<UzumCategory>
    ) {
        for (childrenRecord in childCategoryRecords) {
            val childCategory = CategoryTreeDocument(
                categoryId = childrenRecord.categoryId,
                parentCategoryId = currentCategoryRecord.categoryId,
                title = childrenRecord.title,
            )
            categoryTreeDao.saveCategory(childCategory).awaitSingleOrNull()
            if (childrenRecord.childrenList?.isNotEmpty() == true) {
                saveHierarchicalChildCategory(childrenRecord, childrenRecord.childrenList)
            }
        }
    }
}
