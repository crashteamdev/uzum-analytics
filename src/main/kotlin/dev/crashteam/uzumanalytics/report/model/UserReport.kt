//package dev.crashteam.uzumanalytics.report.model
//
//import org.springframework.data.annotation.Id
//import org.springframework.data.redis.core.RedisHash
//import org.springframework.data.redis.core.TimeToLive
//import org.springframework.data.redis.core.index.Indexed
//import java.util.concurrent.TimeUnit
//
//@RedisHash
//data class UserReport(
//    @Id
//    val id: String,
//    @Indexed
//    val userId: String,
//    val reportCount: Int,
//    @TimeToLive(unit = TimeUnit.HOURS)
//    val timeToLive: Int
//)
