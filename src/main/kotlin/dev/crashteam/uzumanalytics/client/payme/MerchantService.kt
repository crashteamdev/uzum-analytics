package dev.crashteam.uzumanalytics.client.payme

import com.googlecode.jsonrpc4j.JsonRpcParam
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl
import dev.crashteam.uzumanalytics.client.currencyapi.CurrencyApiClient
import dev.crashteam.uzumanalytics.client.payme.model.*
import dev.crashteam.uzumanalytics.controller.model.PaymentProvider
import dev.crashteam.uzumanalytics.domain.mongo.PaycomDocument
import dev.crashteam.uzumanalytics.domain.mongo.PaymentDocument
import dev.crashteam.uzumanalytics.exception.TransactionNotFoundException
import dev.crashteam.uzumanalytics.exception.UnableCancelTransactionException
import dev.crashteam.uzumanalytics.exception.UnableCompleteException
import dev.crashteam.uzumanalytics.repository.mongo.PaymentRepository
import dev.crashteam.uzumanalytics.repository.mongo.PaymentSequenceDao
import dev.crashteam.uzumanalytics.service.PaymentService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import uz.paycom.merchant.entity.OrderCancelReason
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@Service
@AutoJsonRpcServiceImpl
class MerchantService(
    private val paymentRepository: PaymentRepository,
    private val paymentSequenceDao: PaymentSequenceDao,
    private val currencyApiClient: CurrencyApiClient,
    private val transactionStateMapper: TransactionStateMapper
) : IMerchantService {

    override suspend fun createTransaction(
        id: String,
        time: Long,
        amount: BigDecimal,
        account: Account
    ): CreateTransactionResult {
        val paymentDocument = paymentRepository.findByPaycomId(id).awaitSingleOrNull()
        if (paymentDocument != null) throw UnableCompleteException()

        val paymentId = UUID.randomUUID().toString()
        val orderId = paymentSequenceDao.getNextSequenceId(PaymentService.PAYMENT_SEQ_KEY)
        val currencyApiData = currencyApiClient.getCurrency("UZS").data["UZS"]!!
        val finalAmount = (amount * (currencyApiData.value.setScale(2, RoundingMode.HALF_UP)))
        val paycomDocument =
            PaycomDocument(
                paycomId = id, createTime = time, account = account,
                performTime = null, amount = amount, cancelTime = null
            )
        val payment = PaymentDocument(
            paymentId = paymentId,
            orderId = orderId,
            userId = account.user,
            status = "pending",
            paid = false,
            amount = finalAmount,
            subscriptionType = account.order,
            createdAt = LocalDateTime.now(),
            currencyId = "UZS",
            paymentSystem = PaymentProvider.PAYME.name,
            paycomDocument = paycomDocument
        )
        paymentRepository.save(payment).awaitSingleOrNull()
        val transactionState = transactionStateMapper.paymentStatusToTransactionState("pending")
        return CreateTransactionResult(
            transaction = paymentId,
            create_time = time,
            state = transactionState.code,
        )
    }

    override suspend fun performTransaction(id: String): PerformTransactionResult {
        val payment = paymentRepository.findByPaycomId(id).awaitSingleOrNull()
        if (payment != null) {
            if (payment.status == "pending") {
                val paycomDocument = payment.paycomDocument?.copy(performTime = System.currentTimeMillis())
                val updatedPayment = payment.copy(paid = true, status = "success", paycomDocument = paycomDocument)
                paymentRepository.save(updatedPayment).awaitSingleOrNull()
                return PerformTransactionResult(
                    transaction = updatedPayment.paymentId,
                    perform_time = System.currentTimeMillis(),
                    state = transactionStateMapper.paymentStatusToTransactionState("success").code
                )
            } else if (payment.status == "success") {
                return PerformTransactionResult(
                    transaction = payment.paymentId,
                    perform_time = payment.paycomDocument?.performTime,
                    state = transactionStateMapper.paymentStatusToTransactionState("success").code
                )
            }
        }
        throw TransactionNotFoundException()
    }

    override suspend fun cancelTransaction(id: String, reason: OrderCancelReason): CancelTransactionResult {
        val paymentDocument = paymentRepository.findByPaycomId(id).awaitSingleOrNull()
        if (paymentDocument != null) {
            if (paymentDocument.status == "pending") {
                val cancelTime = System.currentTimeMillis()
                val paycomDocument = paymentDocument.paycomDocument?.copy(cancelTime = cancelTime)
                val document = paymentDocument.copy(status = "canceled", paycomDocument = paycomDocument)

                paymentRepository.save(document).awaitSingle()
                return CancelTransactionResult(
                    transaction = document.paymentId,
                    cancel_time = cancelTime,
                    state = transactionStateMapper.paymentStatusToTransactionState("canceled").code
                )
            } else if (paymentDocument.status == "success" || paymentDocument.status == "canceled") {
                throw UnableCancelTransactionException()
            }
        }
        throw TransactionNotFoundException()
    }

    override fun getStatement(
        @JsonRpcParam(value = "from") from: Long,
        @JsonRpcParam(value = "to") to: Long
    ): Transactions {
        val result = mutableListOf<GetStatementResult>()
        val fromTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(from), ZoneId.systemDefault())
        val toTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(to), ZoneId.systemDefault())
        val paymentDocuments =
            paymentRepository.findByPaycomDocumentNotNullAndCreatedAtBetween(fromTime, toTime).collectList().block()
        paymentDocuments?.forEach {
            result.add(
                GetStatementResult(
                    id = it.paycomDocument?.paycomId,
                    create_time = it.paycomDocument?.createTime,
                    amount = it.amount,
                    account = Account(user = it.userId, it.subscriptionType),
                    cancel_time = it.paycomDocument?.cancelTime,
                    perform_time = it.paycomDocument?.performTime,
                    state = transactionStateMapper.paymentStatusToTransactionState(it.status).code
                )
            )
        }
        return Transactions(result)
    }

}