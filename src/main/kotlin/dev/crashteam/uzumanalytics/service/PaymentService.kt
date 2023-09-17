package dev.crashteam.uzumanalytics.service

import dev.crashteam.uzumanalytics.client.currencyapi.CurrencyApiClient
import dev.crashteam.uzumanalytics.client.freekassa.FreeKassaClient
import dev.crashteam.uzumanalytics.client.freekassa.model.PaymentFormRequestParams
import dev.crashteam.uzumanalytics.client.qiwi.QiwiClient
import dev.crashteam.uzumanalytics.client.qiwi.model.QiwiPaymentRequestParams
import dev.crashteam.uzumanalytics.client.uzumbank.UzumBankClient
import dev.crashteam.uzumanalytics.client.uzumbank.model.UzumBankCreatePaymentRequest
import dev.crashteam.uzumanalytics.client.uzumbank.model.UzumBankPayType
import dev.crashteam.uzumanalytics.client.uzumbank.model.UzumBankPaymentParams
import dev.crashteam.uzumanalytics.client.uzumbank.model.UzumBankViewType
import dev.crashteam.uzumanalytics.controller.model.PaymentProvider
import dev.crashteam.uzumanalytics.domain.mongo.*
import dev.crashteam.uzumanalytics.extensions.mapToSubscription
import dev.crashteam.uzumanalytics.repository.mongo.PaymentRepository
import dev.crashteam.uzumanalytics.repository.mongo.PaymentSequenceDao
import dev.crashteam.uzumanalytics.repository.mongo.ReferralCodeRepository
import dev.crashteam.uzumanalytics.repository.mongo.UserRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mu.KotlinLogging
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
    private val uzumBankClient: UzumBankClient,
    private val currencyApiClient: CurrencyApiClient,
) {

    suspend fun createFreekassaPayment(
        userId: String,
        userSubscription: UserSubscription,
        ip: String,
        email: String,
        currencySymbolCode: String,
        referralCode: String? = null,
        multiply: Short? = null
    ): String {
        val paymentId = UUID.randomUUID().toString()
        val isUserCanUseReferral = if (referralCode != null) isUserReferralCodeAccess(userId, referralCode) else false
        val amount = calculatePriceAmount(userSubscription, isUserCanUseReferral, multiply)
        val currencyApiData = currencyApiClient.getCurrency().data["RUB"]!!
        val finalAmount = (amount * (currencyApiData.value.setScale(2, RoundingMode.HALF_UP)))
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
        val amount = calculatePriceAmount(userSubscription, isUserCanUseReferral, multiply)
        val subscriptionName = when (userSubscription) {
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

    suspend fun createUzumBankPayment(
        userId: String,
        userSubscription: UserSubscription,
        referralCode: String? = null,
        multiply: Short? = null,
    ): String {
        val paymentId = UUID.randomUUID().toString()
        val isUserCanUseReferral = if (referralCode != null) isUserReferralCodeAccess(userId, referralCode) else false
        val amount = calculatePriceAmount(userSubscription, isUserCanUseReferral, multiply)
        val currencyApiData = currencyApiClient.getCurrency("UZS").data["UZS"]!!
        val finalAmount = (amount * (currencyApiData.value.setScale(2, RoundingMode.HALF_UP)))
        val paymentResponse = uzumBankClient.createPayment(
            UzumBankCreatePaymentRequest(
                amount = finalAmount.toLong(),
                clientId = userId,
                currency = 860,
                orderNumber = paymentId,
                viewType = UzumBankViewType.REDIRECT,
                paymentParams = UzumBankPaymentParams(UzumBankPayType.ONE_STEP),
                sessionTimeoutSecs = 600,
                successUrl = "https://lk.marketdb.org/#/payment/success",
                failureUrl = "https://lk.marketdb.org/#/payment/error"
            )
        )
        val orderId = paymentSequenceDao.getNextSequenceId(PAYMENT_SEQ_KEY)
        val paymentDocument = PaymentDocument(
            paymentId = paymentId,
            orderId = orderId,
            externalId = paymentResponse.result?.orderId,
            userId = userId,
            status = "pending",
            paid = false,
            amount = finalAmount,
            subscriptionType = userSubscription.num,
            multiply = multiply,
            referralCode = referralCode,
            createdAt = LocalDateTime.now(),
            currencyId = "UZS",
            paymentSystem = PaymentProvider.UZUM_BANK.name,
        )
        paymentRepository.save(paymentDocument).awaitSingleOrNull()

        return paymentResponse.result!!.paymentRedirectUrl
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

    private fun calculatePriceAmount(
        userSubscription: UserSubscription,
        referralCode: Boolean = false,
        multiply: Short? = null
    ): BigDecimal {
        return if (multiply != null && multiply > 1) {
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

    suspend fun findPayment(paymentId: String): PaymentDocument? {
        return paymentRepository.findByPaymentId(paymentId).awaitSingleOrNull()
    }

    @Transactional
    suspend fun uzumCallbackPayment(
        paymentId: String,
    ) {
        val payment = findPayment(paymentId)!!
        val userId = payment.userId
        return callbackPayment(paymentId, userId, "UZS")
    }

    @Transactional
    suspend fun callbackPayment(
        paymentId: String,
        userId: String,
        currencyId: String,
    ) {
        val payment = findPayment(paymentId)!!
        val user = userRepository.findByUserId(userId).awaitSingleOrNull()
        val userSubscription = payment.subscriptionType.mapToSubscription()!!
        if (payment.daysPaid != null) {
            upgradeUserSubscription(
                user!!, userSubscription, paymentId, true, "success"
            )
        } else {
            val subDays = if (payment.multiply != null && payment.multiply > 1) {
                30 * payment.multiply
            } else 30
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
        currencyId: String,
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

//    suspend fun createUpgradeUserSubscriptionPayment(
//        userDocument: UserDocument,
//        upgradeTarget: UserSubscription,
//        paymentRedirectUrl: String
//    ): PaymentResponse {
//        val currentUserSubscription = userDocument.subscription!!
//        val daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), currentUserSubscription.endAt)
//        val currentUserSub = currentUserSubscription.subType?.mapToSubscription()!!
//        val alreadyPayed = currentUserSub.price() - (currentUserSub.price() / 30) * (30 - daysLeft)
//        val newSubPrice = (upgradeTarget.price() / 30) * daysLeft
//        val upgradePrice = newSubPrice - alreadyPayed
//
//        val paymentRequest = PaymentRequest(
//            amount = PaymentAmount(
//                upgradePrice.toString(),
//                "RUB"
//            ),
//            capture = true,
//            confirmation = PaymentConfirmation("redirect", paymentRedirectUrl),
//            createdAt = LocalDateTime.now(),
//            description = "Upgrade subscription from ${currentUserSub.name} to ${upgradeTarget.name}",
//            metadata = mapOf("sub_type" to upgradeTarget.name)
//        )
//        val paymentResponse = youKassaClient.createPayment(UUID.randomUUID().toString(), paymentRequest)
//        val paymentDocument = PaymentDocument(
//            paymentResponse.id,
//            userDocument.userId,
//            paymentResponse.status,
//            paymentResponse.paid,
//            BigDecimal(upgradePrice).setScale(2),
//            upgradeTarget.num,
//            daysLeft.toInt()
//        )
//        paymentRepository.save(paymentDocument).awaitSingleOrNull()
//
//        return paymentResponse
//    }

    companion object {
        const val PAYMENT_SEQ_KEY = "payment"
    }
}
