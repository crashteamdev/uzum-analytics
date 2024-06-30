package dev.crashteam.uzumanalytics.controller

import dev.crashteam.uzumanalytics.controller.model.ProductPositionHistoryView
import dev.crashteam.uzumanalytics.controller.model.ProductPositionView
import dev.crashteam.uzumanalytics.repository.postgres.UserRepository
import dev.crashteam.uzumanalytics.service.ProductServiceV2
import dev.crashteam.uzumanalytics.service.UserRestrictionService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@RestController
@RequestMapping(path = ["v1"], produces = [MediaType.APPLICATION_JSON_VALUE])
class MarketDbApiController(
    private val userRepository: UserRepository,
    private val userRestrictionService: UserRestrictionService,
    private val productService: ProductServiceV2
) {

    @GetMapping("/category/{category_id}/product/{product_id}/sku/{sku_id}/positions")
    suspend fun getProductPositions(
        @PathVariable(name = "category_id") categoryId: Long,
        @PathVariable(name = "product_id") productId: Long,
        @PathVariable(name = "sku_id") skuId: Long,
        @RequestParam(name = "from_time") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) fromTime: LocalDateTime,
        @RequestParam(name = "to_time") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) toTime: LocalDateTime,
        @RequestHeader(name = "X-API-KEY", required = true) apiKey: String,
    ): ResponseEntity<ProductPositionView> {
        if (!checkRequestDaysPermission(apiKey, fromTime, toTime)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val productPositions = productService.getProductPosition(categoryId, productId, skuId, fromTime, toTime)
        if (productPositions.isEmpty()) {
            return ResponseEntity.ok(null)
        }

        return ResponseEntity.ok(ProductPositionView(
            categoryId = categoryId,
            productId = productId,
            skuId = skuId,
            history = productPositions.map {
                ProductPositionHistoryView(
                    position = it.position,
                    date = it.date
                )
            }
        ))
    }

    private suspend fun checkRequestDaysPermission(
        apiKey: String,
        fromTime: LocalDateTime,
        toTime: LocalDateTime
    ): Boolean {
        val daysCount = ChronoUnit.DAYS.between(fromTime, toTime)
        if (daysCount <= 0) return true
        val user = userRepository.findByApiKey_HashKey(apiKey)
            ?: throw IllegalStateException("User not found")
        val checkDaysAccess = userRestrictionService.checkDaysAccess(user, daysCount.toInt())
        if (checkDaysAccess == UserRestrictionService.RestrictionResult.PROHIBIT) {
            return false
        }
        val checkDaysHistoryAccess = userRestrictionService.checkDaysHistoryAccess(user, fromTime)
        if (checkDaysHistoryAccess == UserRestrictionService.RestrictionResult.PROHIBIT) {
            return false
        }
        return true
    }

}
