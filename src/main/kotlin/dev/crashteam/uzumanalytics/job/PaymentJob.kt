//package dev.crashteam.uzumanalytics.job
//
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.reactor.awaitSingle
//import kotlinx.coroutines.reactor.awaitSingleOrNull
//import kotlinx.coroutines.runBlocking
//import mu.KotlinLogging
//import dev.crashteam.uzumanalytics.client.yookassa.YooKassaClient
//import dev.crashteam.uzumanalytics.extensions.getApplicationContext
//import dev.crashteam.uzumanalytics.extensions.mapToSubscription
//import dev.crashteam.uzumanalytics.repository.mongo.PaymentRepository
//import dev.crashteam.uzumanalytics.repository.mongo.UserRepository
//import dev.crashteam.uzumanalytics.service.PaymentService
//import org.quartz.Job
//import org.quartz.JobExecutionContext
//import org.springframework.http.HttpStatus
//import org.springframework.web.client.HttpClientErrorException
//import java.util.concurrent.atomic.AtomicInteger
//
//private val log = KotlinLogging.logger {}
//
//class PaymentJob : Job {
//
//    override fun execute(context: JobExecutionContext) {
//        val appContext = context.getApplicationContext()
//        val yooKassaClient = appContext.getBean(YooKassaClient::class.java)
//        val paymentRepository = appContext.getBean(PaymentRepository::class.java)
//        val userRepository = appContext.getBean(UserRepository::class.java)
//        val paymentId = context.jobDetail.jobDataMap["paymentId"] as? String
//            ?: throw IllegalStateException("paymentId can't be null")
//        val userId = context.jobDetail.jobDataMap["userId"] as? String
//            ?: throw IllegalStateException("userId can't be null")
//        val subscriptionType: Int = context.jobDetail.jobDataMap["subscriptionType"] as? Int
//            ?: throw IllegalStateException("subscriptionType can't be null")
//        val userSubscription = subscriptionType.mapToSubscription()
//            ?: throw IllegalArgumentException("Unknown subscription type $subscriptionType")
//        val pendingRetry = AtomicInteger()
//        runBlocking {
//            log.info { "Check status for paymentId=$paymentId" }
//            val paymentService = appContext.getBean(PaymentService::class.java)
//            while (true) {
//                val checkStatusResponse = try {
//                    yooKassaClient.checkStatus(paymentId)
//                } catch (e: HttpClientErrorException) {
//                    if (e.statusCode == HttpStatus.NOT_FOUND) {
//                        log.warn { "Payment not found. Kill payment with unknown status. PaymentId=$paymentId" }
//                        val payment = paymentRepository.findByPaymentId(paymentId).awaitSingle()
//                        val updatedPayment =
//                            payment.copy(paid = false, status = "unknown")
//                        paymentRepository.save(updatedPayment).awaitSingleOrNull()
//                    }
//                    log.error(e) { "Exception during payment check status. PaymentId=$paymentId" }
//                    null
//                } ?: break
//
//                if (checkStatusResponse.status == "succeeded") {
//                    log.info { "Payment $paymentId in succeeded status. Trying to give user role" }
//                    val payment = paymentRepository.findByPaymentId(paymentId).awaitSingle()
//
//                    // Check if this upgrade payment
//                    if (payment.daysPaid != null) {
//                        val user = userRepository.findByUserId(userId).awaitSingle()
//                        paymentService.upgradeUserSubscription(
//                            user, userSubscription, paymentId, checkStatusResponse.paid, checkStatusResponse.status
//                        )
//                        break
//                    }
//
//                    // TODO: check
////                    val expectedPrice = if (payment.multiply != null && payment.multiply > 1) {
////                        val multipliedAmount = BigDecimal(userSubscription.price()) * BigDecimal.valueOf(payment.multiply.toLong())
////                        (multipliedAmount - (multipliedAmount * BigDecimal(0.10)))
////                    } else {
////                        BigDecimal(userSubscription.price())
////                    }
//
////                    if (BigDecimal(checkStatusResponse.amount.value) < expectedPrice) {
////                        val paymentAmount = BigDecimal(checkStatusResponse.amount.value)
////                        log.warn { "Bad subscription payment amount - ${BigDecimal(checkStatusResponse.amount.value)}" }
////                        val updatedPayment = payment.copy(
////                            paid = checkStatusResponse.paid,
////                            status = checkStatusResponse.status,
////                            amount = paymentAmount
////                        )
////                        paymentRepository.save(updatedPayment).awaitSingleOrNull()
////                        break
////                    }
//                    val subDays = if (payment.multiply != null && payment.multiply > 1) {
//                        30 * payment.multiply
//                    } else {
//                        30
//                    }
//
//                    val user = userRepository.findByUserId(userId).awaitSingleOrNull()
//                    paymentService.saveUserWithSubscription(
//                        paymentId,
//                        userId,
//                        user,
//                        userSubscription,
//                        subDays.toLong(),
//                        checkStatusResponse.paid,
//                        checkStatusResponse.status,
//                        referralCode = payment.referralCode
//                    )
//                    break
//                } else if (checkStatusResponse.status == "pending") {
//                    log.info { "Payment $paymentId in pending status" }
//                    if (pendingRetry.getAndIncrement() > MAX_PENDING_RETRY) break
//                    delay(10000)
//                    continue
//                } else {
//                    log.info { "Payment $paymentId in ${checkStatusResponse.status}" }
//                    val payment = paymentRepository.findByPaymentId(paymentId).awaitSingle()
//                    val updatedPayment =
//                        payment.copy(paid = checkStatusResponse.paid, status = checkStatusResponse.status)
//                    paymentRepository.save(updatedPayment).awaitSingleOrNull()
//                    break
//                }
//            }
//        }
//    }
//
//    private companion object {
//        const val MAX_PENDING_RETRY = 10
//    }
//}
