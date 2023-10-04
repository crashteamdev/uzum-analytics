package dev.crashteam.uzumanalytics.converter.view

import dev.crashteam.uzumanalytics.converter.DataConverter
import dev.crashteam.uzumanalytics.domain.mongo.PromoCodeDocument
import dev.crashteam.uzumanalytics.domain.mongo.PromoCodeType
import dev.crashteam.openapi.uzumanalytics.model.AdditionalTimePromoCode
import dev.crashteam.openapi.uzumanalytics.model.DiscountPromoCode
import dev.crashteam.openapi.uzumanalytics.model.PromoCode
import dev.crashteam.openapi.uzumanalytics.model.PromoCodeContext
import org.springframework.stereotype.Component
import java.time.ZoneOffset

@Component
class PromoCodeToViewConverter : DataConverter<PromoCodeDocument, PromoCode> {

    override fun convert(source: PromoCodeDocument): PromoCode {
        val promoCodeContext: PromoCodeContext = when (source.type) {
            PromoCodeType.DISCOUNT -> {
                val discountPromoCode = DiscountPromoCode(source.discount!!.toInt())
                discountPromoCode.type = PromoCodeContext.TypeEnum.DISCOUNT
                discountPromoCode
            }
            PromoCodeType.ADDITIONAL_DAYS -> {
                val additionalTimePromoCode = AdditionalTimePromoCode(source.additionalDays)
                additionalTimePromoCode.type = PromoCodeContext.TypeEnum.ADDITIONAL_TIME
                additionalTimePromoCode
            }
        }
        return PromoCode(
            source.code,
            source.description,
            source.validUntil.atOffset(ZoneOffset.UTC),
            source.useLimit,
            promoCodeContext
        )
    }
}
