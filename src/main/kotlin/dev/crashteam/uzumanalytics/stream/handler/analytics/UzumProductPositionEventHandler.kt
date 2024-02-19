package dev.crashteam.uzumanalytics.stream.handler.analytics

import com.google.protobuf.Timestamp
import dev.crashteam.uzum.scrapper.data.v1.UzumProductCategoryPositionChange
import dev.crashteam.uzum.scrapper.data.v1.UzumScrapperEvent
import dev.crashteam.uzumanalytics.domain.mongo.ProductPositionId
import dev.crashteam.uzumanalytics.domain.mongo.ProductPositionMetadata
import dev.crashteam.uzumanalytics.domain.mongo.ProductPositionTSDocument
import dev.crashteam.uzumanalytics.extension.toLocalDateTime
import dev.crashteam.uzumanalytics.repository.clickhouse.CHProductPositionRepository
import dev.crashteam.uzumanalytics.repository.clickhouse.model.ChProductPosition
import dev.crashteam.uzumanalytics.repository.mongo.ProductPositionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant

private val log = KotlinLogging.logger {}

@Component
class UzumProductPositionEventHandler(
    private val productPositionRepository: ProductPositionRepository,
    private val chProductPositionRepository: CHProductPositionRepository
) : UzumScrapEventHandler {

    override fun handle(events: List<UzumScrapperEvent>) {
        runBlocking {
            val uzumProductPositionEventWrappers =
                events.map { UzumProductPositionEventWrapper(it.eventPayload.uzumProductPositionChange, it.scrapTime) }
            val oldSaveProductPositionTask = async {
                try {
                    oldSaveProductPosition(uzumProductPositionEventWrappers)
                } catch (e: Exception) {
                    log.error(e) { "Exception during save old schema product position events" }
                }
            }
            val saveProductPositionTask = async {
                try {
                    saveProductPosition(uzumProductPositionEventWrappers)
                } catch (e: Exception) {
                    log.error(e) { "Exception during save product position events" }
                }
            }
            awaitAll(oldSaveProductPositionTask, saveProductPositionTask)
        }
        try {
            val uzumProductPositionEventWrappers =
                events.map { UzumProductPositionEventWrapper(it.eventPayload.uzumProductPositionChange, it.scrapTime) }
            val chProductPositions = uzumProductPositionEventWrappers.map {
                ChProductPosition(
                    fetchTime = it.eventTime.toLocalDateTime(),
                    productId = it.productCategoryPositionChange.productId,
                    skuId = it.productCategoryPositionChange.skuId,
                    categoryId = it.productCategoryPositionChange.categoryId,
                    position = it.productCategoryPositionChange.position
                )
            }
            chProductPositionRepository.saveProductsPosition(chProductPositions)
        } catch (e: Exception) {
            log.error(e) { "Exception during handle position event" }
        }
    }

    private fun saveProductPosition(uzumProductPositionEventWrappers: List<UzumProductPositionEventWrapper>) {
        val chProductPositions = uzumProductPositionEventWrappers.map {
            ChProductPosition(
                fetchTime = it.eventTime.toLocalDateTime(),
                productId = it.productCategoryPositionChange.productId,
                skuId = it.productCategoryPositionChange.skuId,
                categoryId = it.productCategoryPositionChange.categoryId,
                position = it.productCategoryPositionChange.position
            )
        }
        chProductPositionRepository.saveProductsPosition(chProductPositions)
        log.info { "Successfully save product position. count=${chProductPositions.size}" }
    }

    private fun oldSaveProductPosition(uzumProductPositionEventWrappers: List<UzumProductPositionEventWrapper>) {
        for (uzumProductPositionEventWrapper in uzumProductPositionEventWrappers) {
            val productCategoryPositionChange = uzumProductPositionEventWrapper.productCategoryPositionChange
            log.info {
                "[OLD] Consume product position record from stream." +
                        " productId=${productCategoryPositionChange.productId};" +
                        " skuId=${productCategoryPositionChange.skuId};" +
                        " position=${productCategoryPositionChange.position}"
            }
            val productPositionTSDocument = ProductPositionTSDocument(
                position = productCategoryPositionChange.position,
                metadata = ProductPositionMetadata(
                    id = ProductPositionId(
                        productId = productCategoryPositionChange.productId,
                        skuId = productCategoryPositionChange.skuId
                    ),
                    categoryId = productCategoryPositionChange.categoryId
                ),
                timestamp = Instant.ofEpochSecond(
                    uzumProductPositionEventWrapper.eventTime.seconds,
                    uzumProductPositionEventWrapper.eventTime.nanos.toLong(),
                )
            )
            productPositionRepository.save(productPositionTSDocument).doOnSuccess {
                log.info {
                    "[OLD] Successfully saved product position. " + "" +
                            " productId=${productCategoryPositionChange.productId};" +
                            " skuId=${productCategoryPositionChange.skuId};" +
                            " position=${productCategoryPositionChange.position}"
                }
            }.subscribe()
        }
    }

    override fun isHandle(event: UzumScrapperEvent): Boolean {
        return event.eventPayload.hasUzumProductPositionChange()
    }

    private data class UzumProductPositionEventWrapper(
        val productCategoryPositionChange: UzumProductCategoryPositionChange,
        val eventTime: Timestamp,
    )
}
