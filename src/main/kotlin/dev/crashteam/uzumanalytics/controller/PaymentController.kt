package dev.crashteam.uzumanalytics.controller

import dev.crashteam.uzumanalytics.client.click.model.ClickRequest
import dev.crashteam.uzumanalytics.client.click.model.ClickResponse
import dev.crashteam.uzumanalytics.config.properties.FreeKassaProperties
import dev.crashteam.uzumanalytics.config.properties.QiwiProperties
import dev.crashteam.uzumanalytics.controller.model.*
import dev.crashteam.uzumanalytics.domain.mongo.PromoCodeType
import dev.crashteam.uzumanalytics.extensions.mapToSubscription
import dev.crashteam.uzumanalytics.repository.mongo.PromoCodeRepository
import dev.crashteam.uzumanalytics.service.PaymentService
import dev.crashteam.uzumanalytics.service.model.CallbackPaymentAdditionalInfo
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mu.KotlinLogging
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import java.security.Principal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping(path = ["v1"], produces = [MediaType.APPLICATION_JSON_VALUE])
class PaymentController(
    private val freeKassaProperties: FreeKassaProperties,
    private val qiwiProperties: QiwiProperties,
    private val paymentService: PaymentService,
    private val promoCodeRepository: PromoCodeRepository,
) {

    @PostMapping("/payment")
    suspend fun createPayment(
        @RequestBody body: PaymentCreate,
        @RequestHeader(name = "Idempotence-Key", required = true) idempotenceKey: String,
        principal: Principal,
        exchange: ServerWebExchange,
    ): ResponseEntity<PaymentCreateResponse> {
        val promoCodeDocument = if (body.promoCode != null) {
            promoCodeRepository.findByCode(body.promoCode).awaitSingleOrNull()
        } else null
        val paymentUrl = when (body.provider) {
            null, PaymentProvider.FREEKASSA -> {
                paymentService.createFreekassaPayment(
                    userId = principal.name,
                    userSubscription = body.subscriptionType.mapToSubscription()!!,
                    ip = exchange.request.headers["X-Real-IP"]?.single() ?: "unknown",
                    email = body.email,
                    referralCode = body.referralCode,
                    multiply = body.multiply,
                    currencySymbolCode = "RUB",
                    promoCode = body.promoCode,
                    promoCodeType = promoCodeDocument?.type
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

            PaymentProvider.CLICK -> {
                paymentService.createClickPayment(
                    userId = principal.name,
                    userSubscription = body.subscriptionType.mapToSubscription()!!,
                    email = body.email,
                    referralCode = body.referralCode,
                    multiply = body.multiply,
                    currencySymbolCode = "UZS",
                    promoCode = body.promoCode,
                    promoCodeType = promoCodeDocument?.type
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
        val promoCode = formData["us_promocode"]?.singleOrNull()
        val promoCodeType = formData["us_promocodeType"]?.singleOrNull()
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
        val paymentAdditionalInfo = if (promoCode != null && promoCodeType != null) {
            CallbackPaymentAdditionalInfo(promoCode, PromoCodeType.valueOf(promoCodeType))
        } else {
            null
        }
        log.debug { "Callback payment additional info: $paymentAdditionalInfo" }
        paymentService.callbackPayment(
            paymentId,
            userId,
            curId,
            paymentAdditionalInfo = paymentAdditionalInfo,
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
    suspend fun callbackUzumPayment(@RequestBody uzumPaymentCallback: UzumPaymentCallback): ResponseEntity<String> {
        log.info { "Callback uzum payment. Body=$uzumPaymentCallback" }
        if (uzumPaymentCallback.operationState == "SUCCESS" && uzumPaymentCallback.operationType != "REFUND") {
            val paymentId = uzumPaymentCallback.orderNumber
            paymentService.uzumCallbackPayment(paymentId)
            return ResponseEntity.ok().build()
        }
        return ResponseEntity.unprocessableEntity().build()
    }

    @PostMapping(
        "/payment/click/callback",
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    suspend fun callbackClickPayment(exchange: ServerWebExchange): ResponseEntity<ClickResponse> {
        val formData = exchange.formData.awaitSingle()
        val clickTransId = formData["click_trans_id"]?.singleOrNull()
        val serviceId = formData["service_id"]?.singleOrNull()
        val clickPaydocId = formData["click_paydoc_id"]?.singleOrNull()
        val merchantTransId = formData["merchant_trans_id"]?.singleOrNull()
        val merchantPrepareId = formData["merchant_prepare_id"]?.singleOrNull()
        val amount = formData["amount"]?.singleOrNull()
        val action = formData["action"]?.singleOrNull()
        val error = formData["error"]?.singleOrNull()
        val errorNote = formData["error_note"]?.singleOrNull()
        val signTime = formData["sign_time"]?.singleOrNull()
        val signString = formData["sign_string"]?.singleOrNull()

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val signTimeFormatted = LocalDateTime.parse(signTime, formatter)

        val referralCode = formData["communal_param"]?.singleOrNull()
        val promoCode = formData["additional_param3"]?.singleOrNull()
        val promoCodeType = formData["additional_param4"]?.singleOrNull()

        log.info { "Callback click payment. Body=$formData" }
        val callbackClickPayment = paymentService.callbackClickPayment(
            ClickRequest(
                clickTransId!!,
                serviceId!!,
                clickPaydocId!!,
                merchantTransId!!,
                merchantPrepareId!!,
                amount!!,
                action!!,
                error!!,
                errorNote!!,
                signTimeFormatted,
                signTime!!,
                signString!!,
                referralCode,
                promoCode,
                promoCodeType
            )
        )
        return ResponseEntity.ok(callbackClickPayment)
    }
}
