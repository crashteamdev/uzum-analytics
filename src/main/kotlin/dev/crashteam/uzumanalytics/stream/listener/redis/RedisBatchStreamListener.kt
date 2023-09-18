package dev.crashteam.uzumanalytics.stream.listener.redis

import org.springframework.data.redis.connection.stream.Record

interface RedisBatchStreamListener<S, V : Record<S, *>> {
    suspend fun onMessage(messages: List<V>)
}
