package dev.crashteam.uzumanalytics.client.payme.model

import java.util.*

class GetStatementResult(val id: String? = null, val time: Date? = null, val amount: Int? = null, val account: Account? = null,
                         val create_time: Date? = null, val perform_time: Date? = null, val cancel_time: Date? = null,
                         val transaction: Long? = null, val state: Int? = null, val reason: Int? = null)