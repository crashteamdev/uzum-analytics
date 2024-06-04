package dev.crashteam.uzumanalytics.domain.mongo

import dev.crashteam.uzumanalytics.domain.ApiKey
import dev.crashteam.uzumanalytics.domain.SubscriptionType
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.MongoId
import java.time.LocalDate
import java.time.LocalDateTime

@Document("users")
data class UserDocument(
    @Indexed(unique = true)
    val userId: String,
    val subscription: SubscriptionDocument? = null,
    val apiKey: ApiKey? = null,
    val email: String? = null,
    val role: UserRole? = null,
    val lastUsageDay: LocalDate? = null,

    @MongoId
    val id: ObjectId = ObjectId(),
)

data class SubscriptionDocument(
    val type: SubscriptionType? = null, // Deprecated
    val subType: String?,
    val createdAt: LocalDateTime,
    val endAt: LocalDateTime,

    @MongoId
    val id: ObjectId = ObjectId(),
)

enum class UserRole {
    ADMIN
}
