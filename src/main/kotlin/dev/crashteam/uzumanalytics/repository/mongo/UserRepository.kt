package dev.crashteam.uzumanalytics.repository.mongo

import dev.crashteam.uzumanalytics.mongo.UserDocument
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Repository
interface UserRepository : ReactiveCrudRepository<UserDocument, String> {

    fun findByUserId(userId: String): Mono<UserDocument>

    fun findByEmail(email: String): Flux<UserDocument>

    fun findByApiKey_HashKey(hashKey: String): Mono<UserDocument>

    fun findBySubscriptionEndAtBefore(date: LocalDateTime): Flux<UserDocument>
}
