package dev.crashteam.uzumanalytics.client.uzum

class UzumClientException(status: Int, rawResponseBody: String, message: String) : RuntimeException(message)
