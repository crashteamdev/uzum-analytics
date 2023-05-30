package dev.crashteam.uzumanalytics.service

import dev.crashteam.uzumanalytics.domain.mongo.DefaultSubscription
import dev.crashteam.uzumanalytics.domain.mongo.ProSubscription
import dev.crashteam.uzumanalytics.domain.mongo.UserDocument
import dev.crashteam.uzumanalytics.extensions.mapToUserSubscription
import org.springframework.data.domain.Range
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

@Service
class UserRestrictionService {

    fun checkShopReportAccess(user: UserDocument, currentReportCount: Int): RestrictionResult {
        if (user.subscription == null || user.subscription.endAt.isBefore(LocalDateTime.now())) {
            return RestrictionResult.PROHIBIT
        }
        val userSubscription = user.subscription.mapToUserSubscription()!!
        if (currentReportCount >= userSubscription.shopReports()) {
            return RestrictionResult.PROHIBIT
        }
        return RestrictionResult.PERMIT
    }

    fun checkCategoryReportAccess(user: UserDocument, currentReportCount: Int): RestrictionResult {
        if (user.subscription == null || user.subscription.endAt.isBefore(LocalDateTime.now())) {
            return RestrictionResult.PROHIBIT
        }
        val userSubscription = user.subscription.mapToUserSubscription()!!
        if (currentReportCount >= userSubscription.categoryReports()) {
            return RestrictionResult.PROHIBIT
        }
        return RestrictionResult.PERMIT
    }

    fun checkDaysAccess(user: UserDocument, daysRequestCount: Int): RestrictionResult {
        if (user.subscription == null || user.subscription.endAt.isBefore(LocalDateTime.now())) {
            return if (daysRequestCount <= DEFAULT_FREE_DAYS) {
                RestrictionResult.PERMIT
            } else RestrictionResult.PROHIBIT
        }
        val userSubscription = user.subscription.mapToUserSubscription()!!
        var days = userSubscription.days()
        // TODO: temp fix. remove after date
        if (userSubscription == DefaultSubscription
            && user.subscription.createdAt.isBefore(
                LocalDateTime.of(2023, Month.APRIL, 20, 23, 59, 59)
            )
        ) {
            days = Range.closed(1, 90)
        }
        if (!days.contains(daysRequestCount)) {
            return RestrictionResult.PROHIBIT
        }
        return RestrictionResult.PERMIT
    }

    fun checkDaysHistoryAccess(user: UserDocument, fromTime: LocalDateTime): RestrictionResult {
        if (user.subscription == null || user.subscription.endAt.isBefore(LocalDateTime.now())) {
            return if (fromTime.toLocalDate() < LocalDate.now().minusDays(DEFAULT_FREE_DAYS.toLong())) {
                RestrictionResult.PROHIBIT
            } else RestrictionResult.PERMIT
        }
        val userSubscription = user.subscription.mapToUserSubscription()!!
        var days = userSubscription.days()
        // TODO: temp fix. remove after date
        if (userSubscription == DefaultSubscription
            && user.subscription.createdAt.isBefore(
                LocalDateTime.of(2023, Month.APRIL, 20, 23, 59, 59)
            )
        ) {
            days = Range.closed(1, 90)
        }
        if (userSubscription != ProSubscription) {
            val minimalSubscriptionDay = LocalDate.now().minusDays(days.upperBound.value.get().toLong() + 1)
            if (fromTime.toLocalDate() < minimalSubscriptionDay) {
                return RestrictionResult.PROHIBIT
            }
        }
        return RestrictionResult.PERMIT
    }

    enum class RestrictionResult {
        PERMIT, PROHIBIT
    }

    companion object {
        const val DEFAULT_FREE_DAYS = 3
    }
}
