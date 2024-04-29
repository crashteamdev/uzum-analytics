package dev.crashteam.uzumanalytics.stream.handler.analytics

import dev.crashteam.uzum.scrapper.data.v1.UzumProductChange
import dev.crashteam.uzum.scrapper.data.v1.UzumScrapperEvent
import dev.crashteam.uzumanalytics.converter.clickhouse.ChUzumProductConverterResultWrapper
import dev.crashteam.uzumanalytics.domain.mongo.*
import dev.crashteam.uzumanalytics.extensions.toLocalDateTime
import dev.crashteam.uzumanalytics.repository.clickhouse.CHProductRepository
import dev.crashteam.uzumanalytics.repository.mongo.SellerRepository
import dev.crashteam.uzumanalytics.service.ProductService
import dev.crashteam.uzumanalytics.service.model.ProductDocumentTimeWrapper
import dev.crashteam.uzumanalytics.stream.handler.model.UzumProductWrapper
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class UzumProductEventHandler(
    private val productService: ProductService,
    private val conversionService: ConversionService,
    private val chProductRepository: CHProductRepository,
    private val sellerRepository: SellerRepository,
) : UzumScrapEventHandler {

    override fun handle(events: List<UzumScrapperEvent>) {
        val uzumProductChanges = events.map { UzumProductWrapper(it.eventPayload.uzumProductChange, it.scrapTime) }
        log.info { "Consumer product records count ${uzumProductChanges.size}" }
        runBlocking {
            val oldSaveProductTask = async {
                try {
                    log.info { "Save ${uzumProductChanges.size} products (OLD)" }
                    val productDocuments = uzumProductChanges.map {
                        ProductDocumentTimeWrapper(
                            productDocument = toOldProductDocument(it.product),
                            scrapTime = it.eventTime.toLocalDateTime(),
                        )
                    }
                    productService.saveProductWithHistory(productDocuments)
                } catch (e: Exception) {
                    log.error(e) { "Exception during save products on OLD SCHEMA" }
                }
            }
            val saveProductTask = async {
                try {
                    log.info { "Save ${uzumProductChanges.size} products (NEW)" }
                    val products = uzumProductChanges.map {
                        conversionService.convert(it, ChUzumProductConverterResultWrapper::class.java)!!
                    }.flatMap { it.result }
                    chProductRepository.saveProducts(products)
                } catch (e: Exception) {
                    log.error(e) { "Exception during save products on NEW SCHEMA" }
                }
            }

            val sellerTask = async {
                try {
                    val sellerDetailDocuments = uzumProductChanges.map {
                        SellerDetailDocument(
                            sellerId = it.product.seller.id,
                            accountId = it.product.seller.accountId,
                            title = it.product.seller.sellerTitle,
                            link = it.product.seller.sellerLink
                        )
                    }.toSet()
                    sellerRepository.saveSellerBatch(sellerDetailDocuments).subscribe()
                } catch (e: Exception) {
                    log.error(e) { "Exception during save seller info" }
                }
            }
            awaitAll(oldSaveProductTask, saveProductTask, sellerTask)
        }
    }

    override fun isHandle(event: UzumScrapperEvent): Boolean {
        return event.eventPayload.hasUzumProductChange()
    }

    private fun toOldProductDocument(productChange: UzumProductChange): ProductDocument {
        return ProductDocument(
            productId = productChange.productId.toLong(),
            title = productChange.title,
            parentCategory = productChange.category.title.trim(),
            ancestorCategories = productCategoriesToAncestorCategories(productChange.category),
            reviewsAmount = productChange.reviewsAmount,
            orderAmount = productChange.orders,
            rOrdersAmount = null,
            rating = if (productChange.rating > 0) productChange.rating.toBigDecimal() else null,
            totalAvailableAmount = productChange.totalAvailableAmount,
            description = productChange.description,
            attributes = productChange.attributesList,
            tags = productChange.tagsList,
            split = productChange.skusList.map { productSku: UzumProductChange.UzumProductSku ->
                productSplitToMongoDomain(productSku)
            },
            seller = SellerDocument(
                id = productChange.seller.id,
                title = productChange.seller.sellerTitle,
                link = productChange.seller.sellerLink,
                description = productChange.seller.description.ifEmpty { null },
                rating = productChange.seller.rating.toBigDecimal(),
                sellerAccountId = productChange.seller.accountId,
                contacts = productChange.seller.contactsList.map { ProductContactDocument(it.type, it.value) }
            ),
        )
    }

    private fun productSplitToMongoDomain(productSku: UzumProductChange.UzumProductSku): ProductSplitDocument {
        return ProductSplitDocument(
            id = productSku.skuId.toLong(),
            availableAmount = productSku.availableAmount,
            fullPrice = if (productSku.fullPrice.isNotEmpty()) productSku.fullPrice.toBigDecimal() else null,
            purchasePrice = productSku.purchasePrice.toBigDecimal(),
            characteristics = productSku.characteristicsList.map {
                ProductSplitCharacteristicDocument(it.title, it.title, it.value)
            },
            photoKey = productSku.photoKey
        )
    }


    private fun productCategoriesToAncestorCategories(
        productCategory: UzumProductChange.UzumProductCategory
    ): List<String> {
        val ancestorCategories: MutableList<String> = ArrayList()
        var currentCategory: UzumProductChange.UzumProductCategory? = productCategory
        while (currentCategory != null) {
            ancestorCategories.add(currentCategory.title.trim())
            if (!currentCategory.hasParent()) break
            currentCategory = currentCategory.parent
        }

        return ancestorCategories
    }
}
