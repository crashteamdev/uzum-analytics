package dev.crashteam.uzumanalytics.stream.handler

import com.google.protobuf.Timestamp
import dev.crashteam.uzum.scrapper.data.v1.UzumProductCategoryPositionChange
import dev.crashteam.uzum.scrapper.data.v1.UzumScrapperEvent
import dev.crashteam.uzumanalytics.domain.mongo.ProductPositionId
import dev.crashteam.uzumanalytics.domain.mongo.ProductPositionMetadata
import dev.crashteam.uzumanalytics.domain.mongo.ProductPositionTSDocument
import dev.crashteam.uzumanalytics.repository.mongo.ProductPositionRepository
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant

private val log = KotlinLogging.logger {}

@Component
class UzumProductPositionEventHandler(
    private val productPositionRepository: ProductPositionRepository,
) : UzumScrapEventHandler {

    override fun handle(events: List<UzumScrapperEvent>) {
        try {
            val uzumProductPositionEventWrappers =
                events.map { UzumProductPositionEventWrapper(it.eventPayload.uzumProductPositionChange, it.scrapTime) }
            for (uzumProductPositionEventWrapper in uzumProductPositionEventWrappers) {
                val productCategoryPositionChange = uzumProductPositionEventWrapper.productCategoryPositionChange
                log.info {
                    "Consume product position record from stream." +
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
                        "Successfully saved product position. " + "" +
                                " productId=${productCategoryPositionChange.productId};" +
                                " skuId=${productCategoryPositionChange.skuId};" +
                                " position=${productCategoryPositionChange.position}"
                    }
                }.subscribe()
            }
        } catch (e: Exception) {
            log.error(e) { "Exception during handle position event" }
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
