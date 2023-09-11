package dev.crashteam.uzumanalytics.client.payme

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl
import dev.crashteam.uzumanalytics.client.payme.model.*
import org.springframework.stereotype.Service
import uz.paycom.merchant.entity.CustomerOrder
import uz.paycom.merchant.entity.OrderCancelReason
import uz.paycom.merchant.entity.OrderTransaction
import dev.crashteam.uzumanalytics.client.payme.model.TransactionState
import dev.crashteam.uzumanalytics.client.payme.model.Transactions
import dev.crashteam.uzumanalytics.exception.*
import dev.crashteam.uzumanalytics.repository.mongo.PaymentRepository
import uz.paycom.merchant.entity.json.result.*
import java.util.*

@Service
@AutoJsonRpcServiceImpl
class MerchantService(private val paymentRepository: PaymentRepository) : IMerchantService {

    private val time_expired = 43_200_000L


    var order: CustomerOrder? = null

    /**
     * http://paycom.uz/api/#merchant-api-metody-createtransaction-sozdanie-finansovoj-tranzakcii
     */
    override fun createTransaction(id: String, time: Date, amount: Int, account: Account): CreateTransactionResult {
        val transaction = transactionRepository.findByPaycomId(id)
        if (transaction == null) {
            if (checkPerformTransaction(amount, account).allow) {
                val newTransaction = OrderTransaction(
                    paycomId = id, paycomTime = time, createTime = Date(),
                    state = TransactionState.STATE_IN_PROGRESS, order = order
                )
                transactionRepository.save(newTransaction)
                return CreateTransactionResult(newTransaction.createTime, newTransaction.id, newTransaction.state.code)
            }
        } else {
            if (transaction.state == TransactionState.STATE_IN_PROGRESS) {
                if (System.currentTimeMillis() - transaction.paycomTime.time > time_expired) {
                    throw UnableCompleteException()
                } else {
                    return CreateTransactionResult(transaction.createTime, transaction.id, transaction.state.code)
                }
            } else {
                throw UnableCompleteException()
            }
        }

        throw UnableCompleteException()
    }

    /**
     * http://paycom.uz/api/#merchant-api-metody-performtransaction-provedenie-finansovoj-tranzakcii
     */
    override fun performTransaction(id: String): PerformTransactionResult {
        val transaction = transactionRepository.findByPaycomId(id)
        transaction?.let {
            if (transaction.state == TransactionState.STATE_IN_PROGRESS) {
                if (System.currentTimeMillis() - transaction.paycomTime.time > time_expired) {
                    transaction.state = TransactionState.STATE_CANCELED
                    transactionRepository.save(transaction)
                    throw UnableCompleteException()
                } else {
                    transaction.state = TransactionState.STATE_DONE
                    transaction.performTime = Date()
                    transactionRepository.save(transaction)
                    return PerformTransactionResult(transaction.id, transaction.performTime, transaction.state.code)
                }
            } else if (transaction.state == TransactionState.STATE_DONE) {
                return PerformTransactionResult(transaction.id, transaction.performTime, transaction.state.code)
            } else {
                throw UnableCompleteException()
            }
        } ?: throw TransactionNotFoundException()
    }

    /**
     * http://paycom.uz/api/#merchant-api-metody-canceltransaction-otmena-finansovoj-tranzakcii
     */
    override fun cancelTransaction(id: String, reason: OrderCancelReason): CancelTransactionResult {
        val transaction = transactionRepository.findByPaycomId(id)
        transaction?.let {
            if (transaction.state == TransactionState.STATE_IN_PROGRESS) {
                transaction.state = TransactionState.STATE_CANCELED
            } else if (transaction.state == TransactionState.STATE_DONE) {
                if (transaction.order?.delivered == true) { //Check, can we cancel a transaction? (example business logic)
                    throw UnableCancelTransactionException()
                } else {
                    transaction.state = TransactionState.STATE_POST_CANCELED
                }
            } else {
                transaction.state = TransactionState.STATE_CANCELED
            }
            transaction.cancelTime = Date()
            transaction.reason = reason
            transactionRepository.save(transaction)

            return CancelTransactionResult(transaction.id, transaction.cancelTime, transaction.state.code)
        } ?: throw TransactionNotFoundException()
    }

    /**
     * http://paycom.uz/api/#merchant-api-metody-checktransaction-proverka-sostoyaniya-finansovoj-tranzakcii
     */
    override fun checkTransaction(id: String): CheckTransactionResult {
        val transaction = transactionRepository.findByPaycomId(id)
        transaction?.let {
            return CheckTransactionResult(
                transaction.createTime, transaction.performTime, transaction.cancelTime,
                transaction.id, transaction.state.code, transaction.reason?.code
            )
        } ?: throw TransactionNotFoundException()
    }

    /**
     * http://paycom.uz/api/#merchant-api-metody-getstatement-informaciya-o-tranzakciyah-merchanta
     */
    override fun getStatement(from: Date, to: Date): Transactions {
        val result = mutableListOf<GetStatementResult>()
        val transactions = transactionRepository.findByPaycomTimeAndState(from, to, TransactionState.STATE_DONE)
        transactions?.forEach {
            result.add(
                GetStatementResult(
                    it.paycomId, it.paycomTime, it.order?.amount, Account(it.order?.id),
                    it.createTime, it.performTime, it.cancelTime, it.id, it.state.code, it.reason?.code
                )
            )
        }

        return Transactions(result)
    }

}