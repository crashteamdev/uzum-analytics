package dev.crashteam.uzumanalytics.client.payme

import dev.crashteam.uzumanalytics.exception.NoSuchStatusException
import org.springframework.stereotype.Component
import dev.crashteam.uzumanalytics.client.payme.model.TransactionState

@Component
class TransactionStateMapper {

    fun paymentStatusToTransactionState(status: String) : TransactionState {
        return when (status) {
            "created" -> TransactionState.STATE_NEW
            "pending" -> TransactionState.STATE_IN_PROGRESS
            "succeeded" -> TransactionState.STATE_DONE
            "canceled" -> TransactionState.STATE_CANCELED
            "post_canceled" -> TransactionState.STATE_POST_CANCELED
            else -> throw NoSuchStatusException()
        }
    }
}