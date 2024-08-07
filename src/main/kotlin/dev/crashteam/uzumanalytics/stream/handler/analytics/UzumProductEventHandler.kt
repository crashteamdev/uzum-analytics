package dev.crashteam.uzumanalytics.stream.handler.analytics

import dev.crashteam.uzum.scrapper.data.v1.UzumScrapperEvent
import dev.crashteam.uzumanalytics.converter.clickhouse.ChUzumProductConverterResultWrapper
import dev.crashteam.uzumanalytics.db.model.tables.pojos.Sellers
import dev.crashteam.uzumanalytics.repository.clickhouse.CHProductRepository
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
    private val conversionService: ConversionService,
    private val chProductRepository: CHProductRepository,
    private val sellerRepository: dev.crashteam.uzumanalytics.repository.postgres.SellerRepository,
) : UzumScrapEventHandler {

    override fun handle(events: List<UzumScrapperEvent>) {
        val uzumProductChanges = events.map { UzumProductWrapper(it.eventPayload.uzumProductChange, it.scrapTime) }
        log.info { "Consumer product records count ${uzumProductChanges.size}" }
        runBlocking {
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
                        Sellers(
                            it.product.seller.id,
                            it.product.seller.accountId,
                            it.product.seller.sellerLink,
                            it.product.seller.sellerTitle,
                        )
                    }.toSet()
                    sellerRepository.saveBatch(sellerDetailDocuments)
                } catch (e: Exception) {
                    log.error(e) { "Exception during save seller info" }
                }
            }
            awaitAll(saveProductTask, sellerTask)
        }
    }

    override fun isHandle(event: UzumScrapperEvent): Boolean {
        return event.eventPayload.hasUzumProductChange()
    }
}
