package dev.crashteam.uzumanalytics.job

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import dev.crashteam.uzumanalytics.client.uzum.UzumClient
import dev.crashteam.uzumanalytics.client.uzum.model.ProductResponse
import dev.crashteam.uzumanalytics.client.uzum.model.ShopGQLQueryResponse
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

class SellerCollectorJob : Job {

    override fun execute(context: JobExecutionContext) {
        val appContext = context.getApplicationContext()
        val uzumClient = appContext.getBean(UzumClient::class.java)
        val conversionService = appContext.getBean(ConversionService::class.java)
        val uzumProperties = appContext.getBean(UzumProperties::class.java)
        val sellerId = context.jobDetail.jobDataMap["sellerId"] as? Long
            ?: throw IllegalStateException("sellerId can't be null")

        runBlocking {
            log.info { "Start seller product collect for $sellerId" }
            var offset = context.jobDetail.jobDataMap["offset"] as? Long ?: 0
            val productService = appContext.getBean(ProductService::class.java)
            val retryTemplate = appContext.getBean(RetryTemplate::class.java)
            while (true) {
                val queryResponse: ShopGQLQueryResponse? = uzumClient.getSellerProductsGQL(sellerId, limit = 48, offset = offset)
                if (queryResponse == null || queryResponse.items.isEmpty()) {
                    log.warn { "Empty seller products. sellerId=$sellerId; offset=$offset" }
                    break
                }
                val productDocuments = queryResponse.items.mapNotNull { item ->
                    delay(Random.nextLong(100, uzumProperties.throttlingMs ?: 2000))
                    val productInfo = try {
                        retryTemplate.execute<ProductResponse, IOException> {
                            uzumClient.getProductInfo(item.catalogCard.productId.toString())
                                ?: throw IllegalStateException("Product response can't be null ${item.catalogCard.productId}")
                        }
                    } catch (e: Exception) {
                        log.error(e) { "Exception during request product info. It will be ignored. productId=${item.catalogCard.productId}" }
                        null
                    }
                    if (productInfo?.payload == null) {
                        log.warn { "Product info payload can't be empty. productId=${item.catalogCard.productId}" }
                        return@mapNotNull null // skip bad product
                    }
                    conversionService.convert(productInfo.payload.data, ProductDocument::class.java)
                }
                productService.saveProductWithHistory(productDocuments)
                offset += 48
                context.jobDetail.jobDataMap["offset"] = offset
            }
            context.jobDetail.jobDataMap["offset"] = 0
            log.info { "Complete seller product collect for $sellerId" }
        }

    }
}
