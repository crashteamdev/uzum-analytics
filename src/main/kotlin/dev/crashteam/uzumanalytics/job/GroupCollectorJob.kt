package dev.crashteam.uzumanalytics.job

import dev.crashteam.uzumanalytics.client.uzum.UzumClient
import dev.crashteam.uzumanalytics.client.uzum.model.RootCategoriesResponse
import dev.crashteam.uzumanalytics.client.uzum.model.SimpleCategory
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import dev.crashteam.uzumanalytics.domain.mongo.CategoryDocument
import dev.crashteam.uzumanalytics.extensions.getApplicationContext
import dev.crashteam.uzumanalytics.repository.mongo.CategoryDao
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
        for (category in children) {
            val categoryDocument = CategoryDocument(
                category.id,
                category.productAmount,
                category.adult,
                category.eco,
                category.title.trim(),
                if (path == null) ",${rootCategory.title.trim()},${category.title.trim()}," else "$path${category.title.trim()},",
                LocalDateTime.now()
            )
            categoryDao.saveCategory(categoryDocument).awaitSingle()
            if (category.children.isNotEmpty()) {
                saveRootChildren(category, category.children, categories, categoryDocument.path, appContext)
            }
//            val childCategory = kazanExpressClient.getCategoryGQL(category.id.toString(), 10, 0)?.data?.makeSearch?.category
//            if (childCategory != null) {
//                saveInnerChildren(childCategory, categories, appContext)
//            }
        }
    }

//    private suspend fun saveInnerChildren(
//        childCategory: CategoryGQLInfo,
//        categories: RootCategoriesResponse,
//        appContext: ApplicationContext,
//    ) {
//        val categoryDao = appContext.getBean(CategoryDao::class.java)
//        if (childCategory.children.isNotEmpty()) {
//            childCategory.children.forEach { category ->
//                val categoryPathList = category.path?.drop(1)
//                val sb = StringBuilder()
//                val categoryRef = AtomicReference<SimpleCategory>()
//                categoryPathList?.forEach { pathId ->
//                    if (categoryRef.get() == null) {
//                        for (category in categories.payload) {
//                            if (category.id == pathId) {
//                                sb.append(",${category.title.trim()}")
//                                categoryRef.set(category)
//                                break
//                            }
//                        }
//                    } else {
//                        if (categoryRef.get().children.isEmpty()) {
//                            val kazanExpressClient = appContext.getBean(KazanExpressClient::class.java)
//                            val response = kazanExpressClient.getCategory(
//                                categoryRef.get().id.toString(),
//                                10,
//                                0
//                            )?.payload?.category
//                            val title = response?.children?.find { it.id == pathId }?.title?.trim()
//                                ?: throw IllegalStateException("Unknown path $pathId")
//                            sb.append(",${title.trim()}")
//                        } else {
//                            for (subCategory in categoryRef.get().children) {
//                                if (subCategory.id == pathId) {
//                                    sb.append(",${subCategory.title.trim()}")
//                                    categoryRef.set(subCategory)
//                                    break
//                                }
//                            }
//                        }
//                    }
//                }
//                sb.append(",")
//                val categoryDocument = CategoryDocument(
//                    category.id,
//                    category.productAmount,
//                    category.adult,
//                    category.eco,
//                    category.title.trim(),
//                    sb.toString(),
//                    LocalDateTime.now()
//                )
//                categoryDao.saveCategory(categoryDocument).awaitSingle()
//            }
//        }
//    }
}
