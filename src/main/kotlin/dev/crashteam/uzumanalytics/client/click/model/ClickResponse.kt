package dev.crashteam.uzumanalytics.client.click.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ClickResponse(
    @JsonProperty("click_trans_id")
    val clickTransId: Long? = null,

    @JsonProperty("merchant_trans_id")
    val merchantTransId: String? = null,

    @JsonProperty("merchant_prepare_id")
    val merchantPrepareId: Long? = null,

    @JsonProperty("merchant_confirm_id")
    val merchantConfirmId: String? = null,

    val error: Long? = null,

    @JsonProperty("error_note")
    val errorNote: String? = null
) {

}