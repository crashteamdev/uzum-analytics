package dev.crashteam.uzumanalytics.repository.redis

class ApiKeyUserSessionInfo {
    var accessFrom: List<ApiKeyAccessFrom>? = null
}

class ApiKeyAccessFrom {
    var ip: String? = null
    var browser: String? = null
}
