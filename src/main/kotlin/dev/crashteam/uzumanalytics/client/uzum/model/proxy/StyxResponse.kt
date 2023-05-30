package dev.crashteam.uzumanalytics.client.uzum.model.proxy

data class StyxResponse<T>(
    val code: Int,
    val originalStatus: Int,
    val message: String? = null,
    val url: String,
    val body: T? = null,
)
