package dev.crashteam.uzumanalytics.controller.model

data class PaymentSubscriptionUpgradeCreate(
    val redirectUrl: String,
    val subscriptionType: Int
)
