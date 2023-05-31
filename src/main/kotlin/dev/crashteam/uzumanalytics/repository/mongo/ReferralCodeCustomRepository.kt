package dev.crashteam.uzumanalytics.repository.mongo

import com.mongodb.client.result.UpdateResult
import dev.crashteam.uzumanalytics.mongo.ReferralCodeDocument
import reactor.core.publisher.Mono

interface ReferralCodeCustomRepository {

    fun addReferralCodeUser(referralCode: String, userId: String, subscriptionName: String): Mono<UpdateResult>

    fun findReferralInvitedUser(userId: String): Mono<ReferralCodeDocument>
}
