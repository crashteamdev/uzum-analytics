package dev.crashteam.uzumanalytics.controller.model

data class PaymentCreate(
    val currency: String,
    val subscriptionType: Int,
    val email: String,
    val multiply: Short? = null,
    val referralCode: String? = null,
    val provider: PaymentProvider? = null,
    val promoCode: String? = null,
)

enum class PaymentProvider {
    QIWI, FREEKASSA
}
