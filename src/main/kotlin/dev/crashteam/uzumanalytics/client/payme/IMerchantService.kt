package dev.crashteam.uzumanalytics.client.payme

import com.googlecode.jsonrpc4j.JsonRpcError
import com.googlecode.jsonrpc4j.JsonRpcErrors
import com.googlecode.jsonrpc4j.JsonRpcParam
import com.googlecode.jsonrpc4j.JsonRpcService
import dev.crashteam.uzumanalytics.client.payme.model.*
import dev.crashteam.uzumanalytics.exception.*
import uz.paycom.merchant.entity.OrderCancelReason
import java.math.BigDecimal
import java.util.*

@JsonRpcService("/api")
interface IMerchantService {

    @JsonRpcErrors(
        JsonRpcError(
            exception = UnableCompleteException::class,
            code = -31008,
            message = "Unable to complete operation",
            data = "transaction"
        )
    )
    suspend fun createTransaction(
        @JsonRpcParam(value = "id") id: String, @JsonRpcParam(value = "time") time: Long,
        @JsonRpcParam(value = "amount") amount: BigDecimal, @JsonRpcParam(value = "account") account: Account
    ): CreateTransactionResult

    @JsonRpcErrors(
        JsonRpcError(
            exception = UnableCompleteException::class,
            code = -31008,
            message = "Unable to complete operation",
            data = "transaction"
        ),
        JsonRpcError(
            exception = TransactionNotFoundException::class,
            code = -31003,
            message = "Order transaction not found",
            data = "transaction"
        )
    )
    suspend fun performTransaction(@JsonRpcParam(value = "id") id: String): PerformTransactionResult

    @JsonRpcErrors(
        JsonRpcError(
            exception = UnableCancelTransactionException::class,
            code = -31007,
            message = "Unable to cancel transaction",
            data = "transaction"
        ),
        JsonRpcError(
            exception = TransactionNotFoundException::class,
            code = -31003,
            message = "Order transaction not found",
            data = "transaction"
        )
    )
    suspend fun cancelTransaction(
        @JsonRpcParam(value = "id") id: String,
        @JsonRpcParam(value = "reason") reason: OrderCancelReason
    ): CancelTransactionResult

    fun getStatement(@JsonRpcParam(value = "from") from: Long, @JsonRpcParam(value = "to") to: Long): Transactions
}
