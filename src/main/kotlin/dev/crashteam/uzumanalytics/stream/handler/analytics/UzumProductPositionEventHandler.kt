package dev.crashteam.uzumanalytics.stream.handler.analytics

import com.google.protobuf.Timestamp
import dev.crashteam.uzum.scrapper.data.v1.UzumProductCategoryPositionChange
import dev.crashteam.uzum.scrapper.data.v1.UzumScrapperEvent
import dev.crashteam.uzumanalytics.extensions.toLocalDateTime
import dev.crashteam.uzumanalytics.repository.clickhouse.CHProductPositionRepository
import dev.crashteam.uzumanalytics.repository.clickhouse.model.ChProductPosition
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class UzumProductPositionEventHandler(
    private val chProductPositionRepository: CHProductPositionRepository
) : UzumScrapEventHandler {

    override fun handle(events: List<UzumScrapperEvent>) {
        runBlocking {
            val uzumProductPositionEventWrappers =
                events.map { UzumProductPositionEventWrapper(it.eventPayload.uzumProductPositionChange, it.scrapTime) }
            val saveProductPositionTask = async {
                try {
                    saveProductPosition(uzumProductPositionEventWrappers)
                } catch (e: Exception) {
                    log.error(e) { "Exception during save product position events" }
                }
            }
            awaitAll(saveProductPositionTask)
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

    override fun isHandle(event: UzumScrapperEvent): Boolean {
        return event.eventPayload.hasUzumProductPositionChange()
    }

    private data class UzumProductPositionEventWrapper(
        val productCategoryPositionChange: UzumProductCategoryPositionChange,
        val eventTime: Timestamp,
    )
}
