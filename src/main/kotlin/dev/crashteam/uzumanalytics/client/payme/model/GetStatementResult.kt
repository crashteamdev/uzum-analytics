package dev.crashteam.uzumanalytics.client.payme.model

import java.math.BigDecimal
import java.util.*

class GetStatementResult(val id: String? = null, val time: Long? = null, val amount: BigDecimal? = null, val account: Account? = null,
                         val create_time: Long? = null, val perform_time: Long? = null, val cancel_time: Long? = null,
                         val transaction: Long? = null, val state: Int? = null, val reason: Int? = null)