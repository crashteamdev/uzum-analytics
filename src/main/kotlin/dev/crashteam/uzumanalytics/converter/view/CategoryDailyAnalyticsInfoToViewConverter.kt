package dev.crashteam.uzumanalytics.converter.view

import dev.crashteam.mp.external.analytics.category.CategoryDailyAnalyticsInfo
import dev.crashteam.uzumanalytics.converter.DataConverter
import dev.crashteam.uzumanalytics.extensions.toMoney
import dev.crashteam.uzumanalytics.extensions.toProtobufDate
import dev.crashteam.uzumanalytics.service.model.CategoryDailyAnalytics
import org.springframework.stereotype.Component

@Component
class CategoryDailyAnalyticsInfoToViewConverter : DataConverter<CategoryDailyAnalytics, CategoryDailyAnalyticsInfo> {

    override fun convert(source: CategoryDailyAnalytics): CategoryDailyAnalyticsInfo {
        return CategoryDailyAnalyticsInfo.newBuilder().apply {
            this.date = source.date.toProtobufDate()
            this.revenue = source.revenue.toMoney()
            this.averageBill = source.averageBill.toMoney()
            this.salesCount = source.salesCount
            this.availableCount = source.availableCount
        }.build()
    }
}
