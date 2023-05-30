package dev.crashteam.uzumanalytics.client.qiwi.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.OffsetDateTime

data class CreatePaymentBody(
    val amount: PaymentAmount,
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ssXXX")
    val expirationDateTime: OffsetDateTime,
    val comment: String,
    val billPaymentMethodsType: List<String>,
    val flags: List<String>,
    val customer: PaymentCustomerInfo,
    val customFields: Map<String, String>
)

data class PaymentAmount(
    val value: String,
    val currency: String
)

data class PaymentCustomerInfo(
    val account: String,
    val email: String
)
