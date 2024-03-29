package dev.crashteam.uzumanalytics.service

import dev.crashteam.uzumanalytics.client.currencyapi.CurrencyApiClient
import dev.crashteam.uzumanalytics.client.freekassa.FreeKassaClient
import dev.crashteam.uzumanalytics.client.freekassa.model.PaymentFormRequestParams
import dev.crashteam.uzumanalytics.client.qiwi.QiwiClient
import dev.crashteam.uzumanalytics.client.qiwi.model.QiwiPaymentRequestParams
import dev.crashteam.uzumanalytics.domain.mongo.*
import dev.crashteam.uzumanalytics.extensions.mapToSubscription
import dev.crashteam.uzumanalytics.repository.mongo.*
import dev.crashteam.uzumanalytics.service.model.CallbackPaymentAdditionalInfo
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val userRepository: UserRepository,
    private val referralCodeRepository: ReferralCodeRepository,
    private val paymentSequenceDao: PaymentSequenceDao,
    private val freeKassaClient: FreeKassaClient,
    private val qiwiClient: QiwiClient,
    private val currencyApiClient: CurrencyApiClient,
    private val promoCodeRepository: PromoCodeRepository,
    private val reactiveMongoTemplate: ReactiveMongoTemplate,
) {

    suspend fun createFreekassaPayment(
        userId: String,
        userSubscription: UserSubscription,
        ip: String,
        email: String,
        currencySymbolCode: String,
        referralCode: String? = null,
        promoCode: String? = null,
        multiply: Short? = null
    ): String {
        val promoCodeDocument = if (promoCode != null) {
            log.debug { "Find promoCode by: $promoCode" }
            promoCodeRepository.findByCode(promoCode).awaitSingleOrNull()
        } else null
        val paymentId = UUID.randomUUID().toString()
        val isUserCanUseReferral = if (referralCode != null) isUserReferralCodeAccess(userId, referralCode) else false
        val amount = calculatePriceAmount(
            userSubscription,
            isUserCanUseReferral,
            promoCode,
            multiply
        )
        log.debug { "Initiate freekassa payment. paymentId=$paymentId. amount=$amount" }
        val currencyApiData = currencyApiClient.getCurrency().data["RUB"]!!
        val finalAmount = (amount * (currencyApiData.value.setScale(2, RoundingMode.HALF_UP)))
        log.debug { "Final payment amount. paymentId=$paymentId. amount=$amount" }
        val orderId = paymentSequenceDao.getNextSequenceId(PAYMENT_SEQ_KEY)
        val paymentDocument = PaymentDocument(
            paymentId = paymentId,
            orderId = orderId,
            userId = userId,
            status = "pending",
            paid = false,
            amount = finalAmount,
            subscriptionType = userSubscription.num,
            multiply = multiply,
            referralCode = referralCode,
            createdAt = LocalDateTime.now(),
            currencyId = currencySymbolCode,
            paymentSystem = "Freekassa"
        )
        paymentRepository.save(paymentDocument).awaitSingleOrNull()

        return freeKassaClient.createPaymentFormUrl(
            PaymentFormRequestParams(
                userId = userId,
                orderId = paymentId,
                email = email,
                amount = finalAmount,
                currency = currencySymbolCode,
                subscriptionId = userSubscription.num,
                referralCode = referralCode,
                promoCode = promoCode,
                promoCodeType = promoCodeDocument?.type,
                multiply = multiply ?: 1
            )
        )
    }

    suspend fun createQiwiPayment(
        userId: String,
        userSubscription: UserSubscription,
        email: String,
        currencySymbolCode: String,
        referralCode: String? = null,
        multiply: Short? = null
    ): String {
        val paymentId = UUID.randomUUID().toString()
        val isUserCanUseReferral = if (referralCode != null) isUserReferralCodeAccess(userId, referralCode) else false
        val orderId = paymentSequenceDao.getNextSequenceId(PAYMENT_SEQ_KEY)
        val amount = calculatePriceAmount(userSubscription, isUserCanUseReferral, null, multiply)
        val subscriptionName = when (userSubscription) {
            DemoSubscription -> "демо"
            DefaultSubscription -> "базовый"
            AdvancedSubscription -> "расширенный"
            ProSubscription -> "профессиональный"
        }
        val currencyApiData = currencyApiClient.getCurrency().data["RUB"]!!
        val finalAmount = (amount * (currencyApiData.value.setScale(2, RoundingMode.HALF_UP)))
        val payUrl = qiwiClient.createPayment(
            QiwiPaymentRequestParams(
                paymentId = paymentId,
                userId = userId,
                email = email,
                amount = finalAmount,
                comment = "Оплата тарифа '$subscriptionName' на ${multiply ?: 1} месяц(а)",
                subscriptionId = userSubscription.num,
                referralCode = referralCode,
                multiply = multiply ?: 1,
                currencySymbolicCode = currencySymbolCode
            )
        ).payUrl
        val paymentDocument = PaymentDocument(
            paymentId = paymentId,
            orderId = orderId,
            userId = userId,
            status = "pending",
            paid = false,
            amount = finalAmount,
            subscriptionType = userSubscription.num,
            multiply = multiply,
            referralCode = referralCode,
            createdAt = LocalDateTime.now(),
            currencyId = currencySymbolCode,
            paymentSystem = "QIWI"
        )
        paymentRepository.save(paymentDocument).awaitSingleOrNull()

        return payUrl
    }

    private suspend fun isUserReferralCodeAccess(userId: String, referralCode: String): Boolean {
        return if (referralCode.isNotBlank()) {
            val userDocument = userRepository.findByUserId(userId).awaitSingleOrNull()
            val userReferralCode = referralCodeRepository.findByUserId(userId).awaitSingleOrNull()
            val inviteReferralCode = referralCodeRepository.findByCode(referralCode).awaitSingleOrNull()
            inviteReferralCode != null &&
                    userReferralCode?.code != inviteReferralCode.code &&
                    userDocument?.subscription == null
        } else false
    }

    private suspend fun calculatePriceAmount(
        userSubscription: UserSubscription,
        referralCode: Boolean = false,
        promoCode: String? = null,
        multiply: Short? = null
    ): BigDecimal {
        return if (promoCode != null) {
            calculatePromoCodePriceAmount(userSubscription, promoCode, multiply ?: 1)
        } else if (multiply != null && multiply > 1) {
            val multipliedAmount = BigDecimal(userSubscription.price()) * BigDecimal.valueOf(multiply.toLong())
            val discount = if (multiply <= 3) {
                BigDecimal(0.10)
            } else if (multiply >= 6) {
                BigDecimal(0.30)
            } else BigDecimal(0.10)
            (multipliedAmount - (multipliedAmount * discount))
        } else if (referralCode) {
            (BigDecimal(userSubscription.price()) - (BigDecimal(userSubscription.price()) * BigDecimal(0.15)))
        } else {
            userSubscription.price().toBigDecimal()
        }
    }


    private suspend fun calculatePromoCodePriceAmount(
        userSubscription: UserSubscription,
        promoCode: String,
        multiply: Short = 1,
    ): BigDecimal {
        val promoCodeDocument = promoCodeRepository.findByCode(promoCode).awaitSingleOrNull()
            ?: return userSubscription.price().toBigDecimal()
        val defaultPrice = userSubscription.price().toBigDecimal() * multiply.toLong().toBigDecimal()
        if (promoCodeDocument.validUntil < LocalDateTime.now()) {
            return defaultPrice
        }
        return when (promoCodeDocument.type) {
            PromoCodeType.ADDITIONAL_DAYS -> {
                defaultPrice
            }

            PromoCodeType.DISCOUNT -> {
                if (promoCodeDocument.numberOfUses >= promoCodeDocument.useLimit) {
                    defaultPrice
                } else {
                    val price = userSubscription.price().toBigDecimal() * multiply.toLong().toBigDecimal()
                    price - ((price * promoCodeDocument.discount!!.toLong().toBigDecimal()) / BigDecimal.valueOf(100))
                }
            }
        }
    }

    suspend fun findPayment(paymentId: String): PaymentDocument? {
        return paymentRepository.findByPaymentId(paymentId).awaitSingleOrNull()
    }

    @Transactional
    suspend fun callbackPayment(
        paymentId: String,
        userId: String,
        currencyId: String? = null,
        paymentAdditionalInfo: CallbackPaymentAdditionalInfo? = null,
    ) {
        val payment = findPayment(paymentId)!!
        val user = userRepository.findByUserId(userId).awaitSingleOrNull()
        val userSubscription = payment.subscriptionType.mapToSubscription()!!
        if (payment.daysPaid != null) {
            upgradeUserSubscription(
                user!!, userSubscription, paymentId, true, "success"
            )
        } else {
            var subDays = if (payment.multiply != null && payment.multiply > 1) {
                30 * payment.multiply
            } else 30
            if (paymentAdditionalInfo != null) {
                val additionalSubDays =
                    callbackPromoCode(paymentAdditionalInfo.promoCode, paymentAdditionalInfo.promoCodeType)
                subDays += additionalSubDays
            }
            if (userSubscription.price().toBigDecimal() != payment.amount && payment.multiply == null) {
                throw IllegalStateException(
                    "Wrong payment amount. subscriptionPrice=${userSubscription.price()};" +
                            " paymentAmount=${payment.amount}"
                )
            }
            saveUserWithSubscription(
                paymentId,
                userId,
                user,
                userSubscription,
                subDays.toLong(),
                true,
                "success",
                referralCode = payment.referralCode,
                currencyId = currencyId
            )
        }
    }

    suspend fun saveUserWithSubscription(
        paymentId: String,
        userId: String,
        user: UserDocument?,
        userSubscription: UserSubscription,
        subscriptionDays: Long,
        paymentPaid: Boolean,
        paymentStatus: String,
        referralCode: String?,
        currencyId: String?,
    ) {
        val saveUser = if (user == null) {
            UserDocument(
                userId,
                SubscriptionDocument(
                    subType = userSubscription.name,
                    createdAt = LocalDateTime.now(),
                    endAt = LocalDateTime.now().plusDays(subscriptionDays)
                )
            )
        } else {
            val currentTime = LocalDateTime.now()
            val endAt = if (user.subscription?.endAt != null && user.subscription.endAt.isAfter(currentTime)) {
                user.subscription.endAt.plusDays(subscriptionDays)
            } else LocalDateTime.now().plusDays(subscriptionDays)
            log.info {
                "User ${user.userId}. Subscription days: $subscriptionDays; " +
                        "End subscription date: $endAt. Old subscription end date=${user.subscription?.endAt}"
            }
            user.copy(
                subscription = SubscriptionDocument(
                    subType = userSubscription.name,
                    createdAt = LocalDateTime.now(),
                    endAt = endAt
                )
            )
        }
        userRepository.save(saveUser).awaitSingleOrNull()

        // Save payment
        val payment = paymentRepository.findByPaymentId(paymentId).awaitSingle()
        val updatedPayment = payment.copy(paid = paymentPaid, status = paymentStatus, currencyId = currencyId)
        paymentRepository.save(updatedPayment).awaitSingleOrNull()

        // Save if user was invited
        if (referralCode != null) {
            val referralCodeDocument = referralCodeRepository.findByCode(referralCode).awaitSingleOrNull()
            if (referralCodeDocument != null) {
                referralCodeRepository.addReferralCodeUser(referralCode, userId, userSubscription.name)
                    .awaitSingleOrNull()
            }
        }
    }

    suspend fun upgradeUserSubscription(
        user: UserDocument,
        userSubscription: UserSubscription,
        paymentId: String,
        paymentPaid: Boolean,
        paymentStatus: String
    ) {
        val upgradeUser = user.copy(
            subscription = user.subscription!!.copy(subType = userSubscription.name)
        )
        userRepository.save(upgradeUser).awaitSingleOrNull()
        val payment = paymentRepository.findByPaymentId(paymentId).awaitSingle()
        val updatedPayment = payment.copy(paid = paymentPaid, status = paymentStatus)
        paymentRepository.save(updatedPayment).awaitSingleOrNull()
    }

    private suspend fun callbackPromoCode(promoCode: String, promoCodeType: PromoCodeType): Int {
        return try {
            val additionalSubDays = if (promoCodeType == PromoCodeType.ADDITIONAL_DAYS) {
                val promoCodeDocument = promoCodeRepository.findByCode(promoCode).awaitSingleOrNull()
                promoCodeDocument?.additionalDays ?: 0
            } else 0
            val query = Query().apply { addCriteria(Criteria.where("code").`is`(promoCode)) }
            val update = Update().inc("numberOfUses", 1)
            reactiveMongoTemplate.findAndModify(query, update, PromoCodeDocument::class.java).awaitSingle()

            additionalSubDays
        } catch (e: Exception) {
            log.error(e) { "Failed to callback promoCode" }
            0
        }
    }

    companion object {
        const val PAYMENT_SEQ_KEY = "payment"
    }
}
