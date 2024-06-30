package dev.crashteam.uzumanalytics.service

import dev.crashteam.uzumanalytics.db.model.enums.SubscriptionType
import dev.crashteam.uzumanalytics.db.model.tables.pojos.Users
import dev.crashteam.uzumanalytics.service.exception.UserSubscriptionGiveawayException
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class UserSubscriptionService(
    private val userRepository: dev.crashteam.uzumanalytics.repository.postgres.UserRepository,
) {

    fun giveawayDemoSubscription(userId: String) {
        val user = userRepository.findByUserId(userId)
        if (user?.subscriptionType != null) {
            throw UserSubscriptionGiveawayException("User $userId already had subscription")
        }
        if (user == null) {
            val newUser = Users().apply {
                this.userId = userId
                this.subscriptionCreatedAt = LocalDateTime.now()
                this.subscriptionType = SubscriptionType.demo
                this.subscriptionEndAt = this.subscriptionCreatedAt.plusDays(3)
            }
            userRepository.save(newUser)
        } else {
            userRepository.updateSubscription(
                userId,
                SubscriptionType.demo,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(3)
            )
        }
    }

}
