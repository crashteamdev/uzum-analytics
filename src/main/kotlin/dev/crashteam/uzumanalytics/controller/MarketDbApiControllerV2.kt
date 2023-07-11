package dev.crashteam.uzumanalytics.controller

import dev.crashteam.openapi.keanalytics.api.CategoryApi
import dev.crashteam.openapi.keanalytics.api.ProductApi
import dev.crashteam.openapi.keanalytics.api.SellerApi
import dev.crashteam.openapi.keanalytics.model.CategoryOverallInfo200Response
import dev.crashteam.openapi.keanalytics.model.GetProductSales200ResponseInner
import dev.crashteam.openapi.keanalytics.model.ProductSkuHistory
import dev.crashteam.openapi.keanalytics.model.Seller
import dev.crashteam.uzumanalytics.service.ProductServiceAnalytics
import dev.crashteam.uzumanalytics.service.SellerService
import mu.KotlinLogging
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
import java.time.OffsetDateTime

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping(path = ["v2"], produces = [MediaType.APPLICATION_JSON_VALUE])
class MarketDbApiControllerV2(
    private val productServiceAnalytics: ProductServiceAnalytics,
    private val sellerService: SellerService,
) : CategoryApi, ProductApi, SellerApi {

    override fun categoryOverallInfo(
        xRequestID: String,
        categoryId: Long,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<CategoryOverallInfo200Response>> {
        val categoryOverallAnalytics = productServiceAnalytics.getCategoryOverallAnalytics(categoryId)
            ?: return ResponseEntity.notFound().build<CategoryOverallInfo200Response>().toMono()
        return ResponseEntity.ok(CategoryOverallInfo200Response().apply {
            this.averagePrice = categoryOverallAnalytics.averagePrice.setScale(2, RoundingMode.HALF_UP).toDouble()
            this.orderCount = categoryOverallAnalytics.orderCount
            this.sellerCount = categoryOverallAnalytics.sellerCount
            this.salesPerSeller = categoryOverallAnalytics.salesPerSeller.setScale(2, RoundingMode.HALF_UP).toDouble()
            this.productZeroSalesCount = categoryOverallAnalytics.productZeroSalesCount
            this.sellersZeroSalesCount = categoryOverallAnalytics.sellersZeroSalesCount
        }).toMono()
    }

    override fun productSkuHistory(
        xRequestID: String,
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
}
