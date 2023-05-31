package dev.crashteam.uzumanalytics.job

import dev.crashteam.uzumanalytics.client.uzum.UzumClient
import dev.crashteam.uzumanalytics.client.uzum.model.RootCategoriesResponse
import dev.crashteam.uzumanalytics.client.uzum.model.SimpleCategory
import dev.crashteam.uzumanalytics.extensions.getApplicationContext
import dev.crashteam.uzumanalytics.mongo.CategoryDocument
import dev.crashteam.uzumanalytics.repository.mongo.CategoryDao
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.springframework.context.ApplicationContext
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

@DisallowConcurrentExecution
class GroupCollectorJob : Job {

    override fun execute(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val uzumClient = applicationContext.getBean(UzumClient::class.java)
        val categories = uzumClient.getRootCategories()
        val payload = categories?.payload
        if (payload == null) {
            log.warn { "Empty root categories response" }
            return
        }
        val categoryDao = applicationContext.getBean(CategoryDao::class.java)
        runBlocking {
            for ((index, simpleCategory) in payload.withIndex()) {
                var productAmount = simpleCategory.productAmount
                if (productAmount <= 0) {
                    try {
                        val category =
                            uzumClient.getCategoryGQL(simpleCategory.id.toString(), 0, 24)?.data?.makeSearch
                        productAmount = category?.total ?: 0
                    } catch (e: Exception) {
                        log.warn(e) { "Failed to get category" }
                        continue
                    }
                }

                // Root Category
                val rootCategoryDocument = CategoryDocument(
                    simpleCategory.id,
                    productAmount,
                    simpleCategory.adult,
                    simpleCategory.eco,
                    simpleCategory.title.trim(),
                    null,
                    LocalDateTime.now()
                )
                categoryDao.saveCategory(rootCategoryDocument).awaitSingle()
                saveRootChildren(simpleCategory, simpleCategory.children, categories, null, applicationContext)
            }
        }
    }

    private suspend fun saveRootChildren(
        rootCategory: SimpleCategory,
        children: List<SimpleCategory>,
        categories: RootCategoriesResponse,
        path: String?,
        appContext: ApplicationContext,
    ) {
        val categoryDao = appContext.getBean(CategoryDao::class.java)
        val uzumClient = appContext.getBean(UzumClient::class.java)
        for (category in children) {
            val categoryPath = if (path == null) {
                ",${rootCategory.title.trim()},${category.title.trim()},"
            } else "$path${category.title.trim()},"
            val categoryDocument = CategoryDocument(
                category.id,
                category.productAmount,
                category.adult,
                category.eco,
                category.title.trim(),
                categoryPath,
                LocalDateTime.now()
            )
            categoryDao.saveCategory(categoryDocument).awaitSingle()
            if (category.children.isNotEmpty()) {
                saveRootChildren(category, category.children, categories, categoryDocument.path, appContext)
            } else {
                val allCategories = uzumClient.getCategoryGQL(category.id.toString(), 0, 0)?.data?.makeSearch
                    ?: throw IllegalStateException("Can't get categories")
                val childCategories = allCategories.categoryTree.filter { it.category.parent?.id == category.id }
                childCategories.forEach { childCategory ->
                    val childCategory = CategoryDocument(
                        childCategory.category.id,
                        null,
                        childCategory.category.adult,
                        category.eco,
                        childCategory.category.title.trim(),
                        "$categoryPath${childCategory.category.title.trim()},",
                        LocalDateTime.now()
                    )
                    categoryDao.saveCategory(childCategory).awaitSingle()
                }
            }
        }
    }
}
