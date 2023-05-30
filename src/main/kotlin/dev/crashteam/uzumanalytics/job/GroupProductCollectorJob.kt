package dev.crashteam.uzumanalytics.job

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import dev.crashteam.uzumanalytics.client.uzum.UzumClient
import dev.crashteam.uzumanalytics.client.uzum.model.CategoryGQLResponseWrapper
import dev.crashteam.uzumanalytics.client.uzum.model.CategoryGQLSearchResponse
import dev.crashteam.uzumanalytics.client.uzum.model.ProductResponse
import dev.crashteam.uzumanalytics.client.uzum.model.UzumGQLQueryResponse
import dev.crashteam.uzumanalytics.config.properties.UzumProperties
import dev.crashteam.uzumanalytics.domain.mongo.ProductDocument
import dev.crashteam.uzumanalytics.extensions.getApplicationContext
import dev.crashteam.uzumanalytics.service.ProductService
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.springframework.core.convert.ConversionService
import org.springframework.retry.support.RetryTemplate
import java.io.IOException
import kotlin.random.Random

private val log = KotlinLogging.logger {}

class GroupProductCollectorJob : Job {

    override fun execute(context: JobExecutionContext) {
        val appContext = context.getApplicationContext()
        val uzumClient = appContext.getBean(UzumClient::class.java)
        val conversionService = appContext.getBean(ConversionService::class.java)
        val categoryId = context.jobDetail.jobDataMap["categoryId"] as? Long
            ?: throw IllegalStateException("categoryId can't be null")

        runBlocking {
            try {
                log.info { "Start category product collect for $categoryId" }
                var offset = context.jobDetail.jobDataMap["offset"] as? Long ?: 0
                val limit = 60
                val productService = appContext.getBean(ProductService::class.java)
                val uzumProperties = appContext.getBean(UzumProperties::class.java)
                val retryTemplate = appContext.getBean(RetryTemplate::class.java)
                while (true) {
                    val category: CategoryGQLSearchResponse? =
                        retryTemplate.execute<UzumGQLQueryResponse<CategoryGQLSearchResponse>?, Exception> {
                            val gqlQueryResponse = uzumClient.getCategoryGQL(categoryId.toString(), offset, limit)
                            if (gqlQueryResponse?.errors?.isNotEmpty() == true) {
                                val foundOffsetError =
                                    gqlQueryResponse.errors.find { it.message == "too big query offset" }
                                if (foundOffsetError != null) {
                                    offset += 48
                                    log.warn {
                                        "Get category error categoryId=${categoryId}; offset=$offset; limit=$limit." +
                                                " ErrorMessages=$gqlQueryResponse?.errors"
                                    }
                                    throw IllegalStateException(
                                        "Failed to retrieve category info." +
                                                " categoryId=${categoryId}; offset=$offset;"
                                    )
                                }
                            }
                            gqlQueryResponse
                        }.data?.makeSearch
                    if (category?.items == null || category.items.isEmpty()) {
                        log.warn { "Empty category products. categoryId=$categoryId; offset=$offset" }
                        break
                    }

                    val productDocuments: List<ProductDocument> = category.items.mapNotNull { product ->
                        Thread.sleep(Random.nextLong(50, uzumProperties.throttlingMs ?: 2000))
                        val productInfo = try {
                            retryTemplate.execute<ProductResponse, IOException> {
                                uzumClient.getProductInfo(product.catalogCard.productId.toString())
                                    ?: throw IllegalStateException("Product response can't be null ${product.catalogCard.productId}")
                            }
                        } catch (e: Exception) {
                            log.error(e) { "Exception during request product info. It will be ignored. productId=${product.catalogCard.productId}" }
                            null
                        }
                        if (productInfo?.payload == null) {
                            log.warn { "Product info payload can't be empty. productId=${product.catalogCard.productId}" }
                            return@mapNotNull null // skip bad product
                        }
                        conversionService.convert(productInfo.payload.data, ProductDocument::class.java)
                    }
                    productService.saveProductWithHistory(productDocuments)
                    offset += 24
                    context.jobDetail.jobDataMap["offset"] = offset
                }
                context.jobDetail.jobDataMap["offset"] = 0
                log.info { "Complete category product collect for $categoryId" }
            } catch (e: Exception) {
                log.error(e) { "Unexpected exception during product collector job" }
            }
        }
    }

}
