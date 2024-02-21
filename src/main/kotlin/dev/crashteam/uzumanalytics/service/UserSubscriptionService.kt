package dev.crashteam.uzumanalytics.service

import dev.crashteam.uzumanalytics.domain.mongo.SubscriptionDocument
import dev.crashteam.uzumanalytics.repository.mongo.UserRepository
import dev.crashteam.uzumanalytics.service.exception.UserSubscriptionGiveawayException
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UserSubscriptionService(
    private val userRepository: UserRepository,
) {

    fun giveawayDemoSubscription(userId: String) = runBlocking {
        val userDocument = userRepository.findByUserId(userId).awaitSingleOrNull()
            ?: throw UserSubscriptionGiveawayException("User $userId not found")
        if (userDocument.subscription != null) {
            throw UserSubscriptionGiveawayException("User $userId already had subscription")
        }
        val updateUserDocument = userDocument.copy(
            subscription = SubscriptionDocument(
                subType = "demo",
                createdAt = LocalDate.now().atStartOfDay(),
                endAt = LocalDate.now().atStartOfDay().plusDays(5)
            )
        )
        userRepository.save(updateUserDocument).awaitSingleOrNull()
    }

}
