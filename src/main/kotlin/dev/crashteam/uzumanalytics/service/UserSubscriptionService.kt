package dev.crashteam.uzumanalytics.service

import dev.crashteam.uzumanalytics.domain.mongo.SubscriptionDocument
import dev.crashteam.uzumanalytics.domain.mongo.UserDocument
import dev.crashteam.uzumanalytics.repository.mongo.UserRepository
import dev.crashteam.uzumanalytics.service.exception.UserSubscriptionGiveawayException
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDate

@Service
class UserSubscriptionService(
    private val userRepository: UserRepository,
) {

    fun giveawayDemoSubscription(userId: String): Mono<UserDocument> {
        return userRepository.findByUserId(userId).flatMap { userDocument ->
            if (userDocument == null) {
                return@flatMap Mono.error(UserSubscriptionGiveawayException("User $userId not found"))
            }
            if (userDocument.subscription != null) {
                return@flatMap Mono.error(UserSubscriptionGiveawayException("User $userId already had subscription"))
            }
            val updateUserDocument = userDocument.copy(
                subscription = SubscriptionDocument(
                    subType = "demo",
                    createdAt = LocalDate.now().atStartOfDay(),
                    endAt = LocalDate.now().atStartOfDay().plusDays(5)
                )
            )
            userRepository.save(updateUserDocument)
        }
    }

}
