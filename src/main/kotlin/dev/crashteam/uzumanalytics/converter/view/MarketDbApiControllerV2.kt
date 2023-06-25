package dev.crashteam.uzumanalytics.controller.converter

import dev.crashteam.openapi.keanalytics.api.SellerApi
import dev.crashteam.openapi.keanalytics.model.Seller
import dev.crashteam.uzumanalytics.service.SellerService
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping(path = ["v2"], produces = [MediaType.APPLICATION_JSON_VALUE])
class MarketDbApiControllerV2(
    private val sellerService: SellerService,
) : SellerApi {

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
}
