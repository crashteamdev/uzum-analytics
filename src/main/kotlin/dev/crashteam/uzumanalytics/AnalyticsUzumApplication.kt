package dev.crashteam.uzumanalytics

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.web.reactive.config.EnableWebFlux

@EnableAsync
@EnableWebFlux
@EnableCaching
@ConfigurationPropertiesScan
@SpringBootApplication
class AnalyticsUzumApplication

fun main(args: Array<String>) {
    runApplication<AnalyticsUzumApplication>(*args)
}
