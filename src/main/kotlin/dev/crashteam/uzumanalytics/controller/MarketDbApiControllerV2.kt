package dev.crashteam.uzumanalytics.controller

import dev.crashteam.openapi.keanalytics.api.CategoryApi
import dev.crashteam.openapi.keanalytics.api.ProductApi
import dev.crashteam.openapi.keanalytics.api.SellerApi
import dev.crashteam.openapi.keanalytics.model.*
import dev.crashteam.uzumanalytics.repository.mongo.UserRepository
import dev.crashteam.uzumanalytics.service.ProductServiceAnalytics
import dev.crashteam.uzumanalytics.service.SellerService
import dev.crashteam.uzumanalytics.service.UserRestrictionService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.kotlin.core.publisher.toFlux
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping(path = ["v2"], produces = [MediaType.APPLICATION_JSON_VALUE])
class MarketDbApiControllerV2(
    private val productServiceAnalytics: ProductServiceAnalytics,
    private val sellerService: SellerService,
    private val userRepository: UserRepository,
    private val userRestrictionService: UserRestrictionService,
) : CategoryApi, ProductApi, SellerApi {

    override fun categoryOverallInfo(
        xRequestID: String,
        X_API_KEY: String,
        categoryId: Long,
        fromTime: OffsetDateTime?,
        toTime: OffsetDateTime?,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<CategoryOverallInfo200Response>> {
        val fromTimeLocalDateTime = fromTime?.toLocalDateTime() ?: LocalDate.now().minusDays(30).atTime(LocalTime.MIN)
        val toTimeLocalDateTime = toTime?.toLocalDateTime() ?: LocalDate.now().atTime(LocalTime.MAX)
        return checkRequestDaysPermission(X_API_KEY, fromTimeLocalDateTime, toTimeLocalDateTime).flatMap { access ->
            if (access == false) {
                ResponseEntity.status(HttpStatus.FORBIDDEN).build<CategoryOverallInfo200Response>().toMono()
            } else {
                val categoryOverallAnalytics = productServiceAnalytics.getCategoryOverallAnalytics(
                    categoryId,
                    fromTimeLocalDateTime,
                    toTimeLocalDateTime
                ) ?: return@flatMap ResponseEntity.notFound().build<CategoryOverallInfo200Response>().toMono()
                return@flatMap ResponseEntity.ok(CategoryOverallInfo200Response().apply {
                    this.averagePrice = categoryOverallAnalytics.averagePrice.setScale(2, RoundingMode.HALF_UP).toDouble()
                    this.orderCount = categoryOverallAnalytics.orderCount
                    this.sellerCount = categoryOverallAnalytics.sellerCount
                    this.salesPerSeller = categoryOverallAnalytics.salesPerSeller.setScale(2, RoundingMode.HALF_UP).toDouble()
                    this.productCount = categoryOverallAnalytics.productCount
                    this.productZeroSalesCount = categoryOverallAnalytics.productZeroSalesCount
                    this.sellersZeroSalesCount = categoryOverallAnalytics.sellersZeroSalesCount
                }).toMono()
            }
        }
    }

    @ExperimentalCoroutinesApi
    override fun sellerOverallInfo(
        xRequestID: String,
        X_API_KEY: String,
        sellerLink: String,
        fromTime: OffsetDateTime,
        toTime: OffsetDateTime,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<SellerOverallInfo200Response>> {
        val fromTimeLocalDateTime = fromTime.toLocalDateTime()
        val toTimeLocalDateTime = toTime.toLocalDateTime()
        return checkRequestDaysPermission(X_API_KEY, fromTimeLocalDateTime, toTimeLocalDateTime).flatMap { access ->
            if (access == false) {
                ResponseEntity.status(HttpStatus.FORBIDDEN).build<SellerOverallInfo200Response>().toMono()
            } else {
                val categoryOverallAnalytics = productServiceAnalytics.getSellerAnalytics(sellerLink, fromTimeLocalDateTime, toTimeLocalDateTime)
                    ?: return@flatMap ResponseEntity.notFound().build<SellerOverallInfo200Response>().toMono()
                return@flatMap ResponseEntity.ok(SellerOverallInfo200Response().apply {
                    this.averagePrice = categoryOverallAnalytics.averagePrice.setScale(2, RoundingMode.HALF_UP).toDouble()
                    this.orderCount = categoryOverallAnalytics.orderCount
                    this.productCount = categoryOverallAnalytics.productCount
                    this.revenue = categoryOverallAnalytics.revenue.toDouble()
                    this.productCountWithSales = categoryOverallAnalytics.productCountWithSales
                    this.salesDynamic = categoryOverallAnalytics.salesDynamic.map { chSellerOrderDynamic ->
                        DynamicSales().apply {
                            date = chSellerOrderDynamic.date
                            orderAmount = chSellerOrderDynamic.orderAmount
                        }
                    }
                }).toMono()
            }
        }
    }

    override fun productSkuHistory(
        xRequestID: String,
        X_API_KEY: String,
        productId: Long,
        skuId: Long,
        fromTime: OffsetDateTime,
        toTime: OffsetDateTime,
        limit: Int,
        offset: Int,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<ProductSkuHistory>>> {
        val productAnalytics = productServiceAnalytics.getProductAnalytics(
            productId,
            skuId,
            fromTime.toLocalDateTime(),
            toTime.toLocalDateTime()
        )
        if (productAnalytics.isEmpty()) {
            return ResponseEntity.notFound().build<Flux<ProductSkuHistory>>().toMono()
        }
        val productSkuHistoryList = productAnalytics.map {
            ProductSkuHistory().apply {
                this.productId = productId
                this.skuId = skuId
                this.name = it.title
                this.orderAmount = it.orderAmount
                this.reviewsAmount = it.reviewAmount
                this.totalAvailableAmount = it.totalAvailableAmount
                this.fullPrice = it.fullPrice?.toDouble()
                this.purchasePrice = it.purchasePrice.toDouble()
                this.availableAmount = it.availableAmount
                this.salesAmount = it.salesAmount.toDouble()
                this.photoKey = it.photoKey
                this.date = it.date
            }
        }
        return ResponseEntity.ok(productSkuHistoryList.toFlux()).toMono()
    }

    override fun getSellerShops(
        sellerLink: String,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<Seller>>> {
        return ResponseEntity.ok(sellerService.findSellersByLink(sellerLink).map {
            Seller().apply {
                this.title = it.title
                this.link = it.link
                this.accountId = it.accountId
            }
        }).toMono().doOnError { log.error(it) { "Failed to get seller shops" } }
    }

    override fun getProductSales(
        xRequestID: String,
        X_API_KEY: String,
        productIds: MutableList<Long>,
        fromTime: OffsetDateTime,
        toTime: OffsetDateTime,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<GetProductSales200ResponseInner>>> {
        val productSalesAnalytics = productServiceAnalytics.getProductSalesAnalytics(
            productIds,
            fromTime.toLocalDateTime(),
            toTime.toLocalDateTime()
        )
        val productSales = productSalesAnalytics.map {
            GetProductSales200ResponseInner().apply {
                this.productId = it.productId.toLong()
                this.salesAmount = it.salesAmount.toDouble()
                this.orderAmount = it.orderAmount
                this.dailyOrder = it.dailyOrderAmount.setScale(2, RoundingMode.HALF_UP).toDouble()
                this.seller = Seller().apply {
                    this.accountId = it.sellerAccountId
                    this.link = it.sellerLink
                    this.title = it.sellerTitle
                }
            }
        }
        return ResponseEntity.ok(productSales.toFlux()).toMono()
    }

    private fun checkRequestDaysPermission(
        apiKey: String,
        fromTime: LocalDateTime,
        toTime: LocalDateTime
    ): Mono<Boolean> {
        val daysCount = ChronoUnit.DAYS.between(fromTime, toTime)
        if (daysCount <= 0) return true.toMono()
        return userRepository.findByApiKey_HashKey(apiKey).flatMap { user ->
            val checkDaysAccess = userRestrictionService.checkDaysAccess(user, daysCount.toInt())
            if (checkDaysAccess == UserRestrictionService.RestrictionResult.PROHIBIT) {
                return@flatMap false.toMono()
            }
            val checkDaysHistoryAccess = userRestrictionService.checkDaysHistoryAccess(user, fromTime)
            if (checkDaysHistoryAccess == UserRestrictionService.RestrictionResult.PROHIBIT) {
                return@flatMap false.toMono()
            }
            true.toMono()
        }
    }
}
