package dev.crashteam.uzumanalytics.extensions

import dev.crashteam.uzumanalytics.db.model.enums.SubscriptionType
import dev.crashteam.uzumanalytics.domain.*
import dev.crashteam.uzumanalytics.domain.mongo.SubscriptionDocument

fun Int.mapToSubscription(): UserSubscription? {
    return UserSubscription::class.sealedSubclasses.firstOrNull {
        it.objectInstance?.num == this
    }?.objectInstance
}

fun String.mapToSubscription(): UserSubscription? {
    return UserSubscription::class.sealedSubclasses.firstOrNull {
        it.objectInstance?.name == this
    }?.objectInstance
}

fun SubscriptionDocument.mapToUserSubscription(): UserSubscription? {
    if (this.type != null && this.subType == null) {
        return DefaultSubscription
    }
    return UserSubscription::class.sealedSubclasses.firstOrNull {
        it.objectInstance?.name == this.subType
    }?.objectInstance
}

fun SubscriptionType.mapToUserSubscription(): UserSubscription {
    return when (this) {
        SubscriptionType.default_ -> DefaultSubscription

        SubscriptionType.advanced -> AdvancedSubscription

        SubscriptionType.pro -> ProSubscription

        SubscriptionType.demo -> DemoSubscription
    }
}

fun String.mapToEntityUserSubscription(): SubscriptionType {
    return when (this) {
        "default" -> SubscriptionType.default_
        "advanced" -> SubscriptionType.advanced
        "pro" -> SubscriptionType.pro
        else -> throw IllegalArgumentException("Unknown type: $this")
    }
}

