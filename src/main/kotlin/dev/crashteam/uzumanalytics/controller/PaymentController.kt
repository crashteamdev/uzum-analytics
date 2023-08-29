package dev.crashteam.uzumanalytics.controller

import dev.crashteam.uzumanalytics.config.properties.FreeKassaProperties
import dev.crashteam.uzumanalytics.config.properties.QiwiProperties
import dev.crashteam.uzumanalytics.controller.model.*
import dev.crashteam.uzumanalytics.extensions.mapToSubscription
import dev.crashteam.uzumanalytics.service.PaymentService
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import java.security.Principal
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping(path = ["v1"], produces = [MediaType.APPLICATION_JSON_VALUE])
class PaymentController(
    private val freeKassaProperties: FreeKassaProperties,
    private val qiwiProperties: QiwiProperties,
    private val paymentService: PaymentService,
) {

    @PostMapping("/payment")
    suspend fun createPayment(
        @RequestBody body: PaymentCreate,
        @RequestHeader(name = "Idempotence-Key", required = true) idempotenceKey: String,
        principal: Principal,
        exchange: ServerWebExchange,
    ): ResponseEntity<PaymentCreateResponse> {
        val paymentUrl = when (body.provider) {
            null, PaymentProvider.FREEKASSA -> {
                paymentService.createFreekassaPayment(
                    userId = principal.name,
                    userSubscription = body.subscriptionType.mapToSubscription()!!,
                    ip = exchange.request.headers["X-Real-IP"]?.single() ?: "unknown",
                    email = body.email,
                    referralCode = body.referralCode,
                    multiply = body.multiply,
                    currencySymbolCode = "RUB"
                )
            }
            PaymentProvider.UZUM_BANK -> {
                paymentService.createUzumBankPayment(
                    userId = principal.name,
                    userSubscription = body.subscriptionType.mapToSubscription()!!,
                    referralCode = body.referralCode,
                    multiply = body.multiply,
                )
            }
            PaymentProvider.QIWI -> {
                paymentService.createQiwiPayment(
                    userId = principal.name,
                    userSubscription = body.subscriptionType.mapToSubscription()!!,
                    email = body.email,
                    referralCode = body.referralCode,
                    multiply = body.multiply,
                    currencySymbolCode = "RUB"
                )
            }
        }
        return ResponseEntity.ok(PaymentCreateResponse(paymentUrl = paymentUrl))
    }

    @PostMapping("/payment/upgrade")
    suspend fun upgradeSubscription(
        @RequestBody body: PaymentSubscriptionUpgradeCreate,
        @RequestHeader(name = "Idempotence-Key", required = true) idempotenceKey: String,
        principal: Principal
    ): ResponseEntity<PaymentCreateResponse> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
    }

    @PostMapping(
        "/payment/callback",
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    suspend fun callbackPayment(
        exchange: ServerWebExchange,
    ): ResponseEntity<String> {
        val formData = exchange.formData.awaitSingle()
        val merchantId = formData["MERCHANT_ID"]?.singleOrNull()
        val amount = formData["AMOUNT"]?.singleOrNull()
        val orderId = formData["MERCHANT_ORDER_ID"]?.singleOrNull()
        val paymentId = formData["us_paymentid"]?.singleOrNull()
        val curId = formData["CUR_ID"]?.singleOrNull()
        val userId = formData["us_userid"]?.singleOrNull()
        val subscriptionId = formData["us_subscriptionid"]?.singleOrNull()
        log.info { "Callback freekassa payment. Body=$formData" }
        if (merchantId == null || amount == null || orderId == null || curId == null
            || userId == null || subscriptionId == null || paymentId == null
        ) {
            log.warn { "Callback payment. Bad request. Body=$formData" }
            return ResponseEntity.badRequest().build()
        }
        val md5Hash =
            DigestUtils.md5Hex("$merchantId:$amount:${freeKassaProperties.secretWordSecond}:$orderId")
        if (formData["SIGN"]?.single() != md5Hash) {
            log.warn { "Callback payment sign is not valid. expected=$md5Hash; actual=${formData["SIGN"]?.single()}" }
            return ResponseEntity.badRequest().build()
        }
        paymentService.callbackPayment(
            paymentId,
            userId,
            curId,
        )

        return ResponseEntity.ok("YES")
    }

    @PostMapping("/payment/qiwi/callback")
    suspend fun callbackQiwiPayment(
        @RequestHeader("Signature") signature: String,
        @RequestBody callbackBody: QiwiPaymentCallbackBody,
    ): ResponseEntity<String> {
        log.info { "Callback qiwi payment. Body=$callbackBody" }

        val concat =
            "${callbackBody.payment.paymentId}|${callbackBody.payment.createdDateTime}|${callbackBody.payment.amount.value}"
        val hmac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(qiwiProperties.callbackSecret.toByteArray(), "HmacSHA256")
        hmac.init(secretKey)
        val hex = Hex.encodeHexString(hmac.doFinal(concat.toByteArray()))
        if (signature != hex) {
            log.warn { "Callback payment sign is not valid. expected=$hex; actual=${signature}" }
            return ResponseEntity.badRequest().build()
        }
        if (callbackBody.payment.status.value == "SUCCESS") {
            val userId = callbackBody.payment.customer.account
            val paymentId = callbackBody.payment.billId
            val currency = callbackBody.payment.amount.currency
            paymentService.callbackPayment(
                paymentId,
                userId,
                currency,
            )
            return ResponseEntity.ok().build()
        }

        return ResponseEntity.unprocessableEntity().build()
    }

    @PostMapping("/payment/uzum/callback")
    suspend fun callbackUzumPayment(@RequestBody uzumPaymentCallback: UzumPaymentCallback) : ResponseEntity<String> {
        log.info { "Callback uzum payment. Body=$uzumPaymentCallback" }
        if (uzumPaymentCallback.operationState == "SUCCESS" && uzumPaymentCallback.operationType != "REFUND") {
            val paymentId = uzumPaymentCallback.orderId
            paymentService.uzumCallbackPayment(paymentId)
            return ResponseEntity.ok().build()
        }
        return ResponseEntity.unprocessableEntity().build()
    }
}
