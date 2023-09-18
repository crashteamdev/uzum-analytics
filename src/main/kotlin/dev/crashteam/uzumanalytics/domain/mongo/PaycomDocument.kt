package dev.crashteam.uzumanalytics.domain.mongo

import dev.crashteam.uzumanalytics.client.payme.model.Account
import java.math.BigDecimal

data class PaycomDocument(
    val paycomId: String, val createTime: Long?,
    val performTime: Long?, val amount: BigDecimal?,
    val account: Account, val cancelTime: Long?
) {
}