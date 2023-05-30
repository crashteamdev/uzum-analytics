package dev.crashteam.uzumanalytics.job

import dev.crashteam.uzumanalytics.client.uzum.UzumClient
import dev.crashteam.uzumanalytics.client.uzum.model.CategoryGQLSearchResponse
import dev.crashteam.uzumanalytics.client.uzum.model.UzumGQLQueryResponse
import dev.crashteam.uzumanalytics.config.properties.UzumProperties
import dev.crashteam.uzumanalytics.domain.mongo.ProductPositionId
import dev.crashteam.uzumanalytics.domain.mongo.ProductPositionMetadata
import dev.crashteam.uzumanalytics.domain.mongo.ProductPositionTSDocument
import dev.crashteam.uzumanalytics.extensions.getApplicationContext
import dev.crashteam.uzumanalytics.repository.mongo.ProductPositionRepository
import io.micrometer.core.instrument.Measurement
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.retry.support.RetryTemplate
import java.time.Instant
import kotlin.random.Random


private val log = KotlinLogging.logger {}

class PositionProductJob : Job {

    override fun execute(jobContext: JobExecutionContext) {
        val applicationContext = jobContext.getApplicationContext()
        val uzumClient = applicationContext.getBean(UzumClient::class.java)
        val uzumProperties = applicationContext.getBean(UzumProperties::class.java)
        val retryTemplate = applicationContext.getBean(RetryTemplate::class.java)
        val categoryId = jobContext.jobDetail.jobDataMap[PositionProductMasterJob.JOB_CATEGORY_ID] as? Long
            ?: throw IllegalStateException("categoryId can't be null")
        var offset = jobContext.jobDetail.jobDataMap["offset"] as? Long ?: 0
        var totalItemProcessed = jobContext.jobDetail.jobDataMap["totalItemProcessed"] as? Long ?: 0
        log.info { "Start position fetch job for categoryId=${categoryId}" }
        val limit = 48
        var position: Long = 0
        while (true) {
            val response = retryTemplate.execute<UzumGQLQueryResponse<CategoryGQLSearchResponse>?, Exception> {
                log.info { "Get category items. categoryId=${categoryId}; offset=$offset; limit=$limit" }
                val gqlQueryResponse = uzumClient.getCategoryGQL(categoryId.toString(), offset, limit)
                if (gqlQueryResponse?.data?.makeSearch == null || gqlQueryResponse.errors?.isNotEmpty() == true) {
                    return@execute null
                }
                if (gqlQueryResponse.data.makeSearch.total <= totalItemProcessed) {
                    log.info {
                        "Total GQL response items - [${gqlQueryResponse.data.makeSearch.total}] less or equal than" +
                                " total processed items - [${totalItemProcessed}] of category - [${categoryId}]"
                    }
                    return@execute null
                }
                gqlQueryResponse
            }.data?.makeSearch

            if (response?.items == null || response.items.isEmpty()) {
                log.warn {"Skipping position job gql request for categoryId - $categoryId with offset - $offset, cause items are empty" }
                offset += limit
                jobContext.jobDetail.jobDataMap["offset"] = offset
                break
            }

            val productItems = response.items
            log.info { "Iterate through product positions for itemsCount=${productItems.size};categoryId=${categoryId}" }
            val productPositionList = mutableListOf<ProductPositionTSDocument>()
            for (productItem in productItems) {
                position += 1
                val productItemCard = productItem.catalogCard
                val productItemCardCharacteristics = productItemCard.characteristicValues
                Thread.sleep(Random.nextLong(50, uzumProperties.throttlingMs ?: 2000))
                val productResponse = uzumClient.getProductInfo(productItemCard.productId.toString())
                if (productResponse?.payload == null) {
                    log.warn { "Empty product data for ${productItemCard.productId}" }
                    continue
                }
                if (productItemCardCharacteristics?.isNotEmpty() == true) {
                    val productItemCardCharacteristic = productItemCardCharacteristics.first()
                    val characteristicId = productItemCardCharacteristic.id
                    val productCharacteristics = productResponse.payload.data.characteristics
                    var indexOfCharacteristic: Int? = null
                    productCharacteristics.forEach { productCharacteristic ->
                        productCharacteristic.values.forEachIndexed { index, productCharacteristicValue ->
                            if (productCharacteristicValue.id == characteristicId) {
                                indexOfCharacteristic = index
                            }
                        }
                    }
                    if (indexOfCharacteristic == null) {
                        log.warn { "Something goes wrong. Can't find index of characteristic." +
                                " productId=${productItemCard.productId}; characteristicId=${characteristicId}" }
                        continue
                    }
                    val skuIds = productResponse.payload.data.skuList!!.filter { productSku ->
                        productSku.characteristics.find { it.valueIndex == indexOfCharacteristic } != null
                                && productSku.availableAmount > 0
                    }.map { it.id }
                    skuIds.forEach { skuId ->
                        val productPositionTSDocument = ProductPositionTSDocument(
                            position = position,
                            metadata = ProductPositionMetadata(
                                id = ProductPositionId(
                                    productId = productItemCard.productId,
                                    skuId = skuId
                                ),
                                categoryId = categoryId
                            ),
                            timestamp = Instant.now()
                        )
                        productPositionList.add(productPositionTSDocument)
                    }
                } else {
                    val skuIds = productResponse.payload.data.skuList!!.filter { productSku ->
                        productSku.availableAmount > 0
                    }.map { it.id }
                    skuIds.forEach { skuId ->
                        val productPositionTSDocument = ProductPositionTSDocument(
                            position = position,
                            metadata = ProductPositionMetadata(
                                id = ProductPositionId(
                                    productId = productItemCard.productId,
                                    skuId = skuId
                                ),
                                categoryId = categoryId
                            ),
                            timestamp = Instant.now()
                        )
                        productPositionList.add(productPositionTSDocument)
                    }
                }
            }
            if (productPositionList.isNotEmpty()) {
                val productPositionRepository = applicationContext.getBean(ProductPositionRepository::class.java)
                runBlocking {
                    log.info { "Save product positions. count=${productPositionList.size};categoryId=${categoryId}" }
                    productPositionRepository.saveAll(productPositionList).awaitLast()
                }
            }

            offset += limit
            totalItemProcessed += productItems.size
            jobContext.jobDetail.jobDataMap["offset"] = offset
            jobContext.jobDetail.jobDataMap["totalItemProcessed"] = totalItemProcessed
        }
    }
}
