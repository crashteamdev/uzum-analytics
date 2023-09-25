package dev.crashteam.uzumanalytics.controller.model

data class UzumPaymentCallback(val orderId: String,
                               val operationState: String,
                               val operationType: String,
                               val merchantOperationId: String,
                               val orderNumber: String,
                               val rrn:String)
