package dev.crashteam.uzumanalytics.client.click

import dev.crashteam.uzumanalytics.client.click.model.ClickPaymentFormRequestParams
import dev.crashteam.uzumanalytics.config.properties.ClickProperties
import org.springframework.stereotype.Component

@Component
class ClickClient(
    private val clickProperties: ClickProperties
) {

    fun createClickUrl(paymentFormRequest: ClickPaymentFormRequestParams): String {
        return buildString {
            append(clickProperties.baseUrl)
            append("?service_id=").append(clickProperties.serviceId)
            append("&merchant_id=").append(clickProperties.merchantId)
            append("&amount=").append(paymentFormRequest.amount)
            append("&transaction_param=").append(paymentFormRequest.paymentId)
            if (paymentFormRequest.referralCode != null) {
                append("&communal_param=${paymentFormRequest.referralCode}")
            }
            if (paymentFormRequest.promoCode != null ) {
                append("&additional_param3=${paymentFormRequest.promoCode}")
            }
        }
    }
}
