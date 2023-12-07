package dev.crashteam.uzumanalytics.stream.listener.aws

import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class UzumEventStreamInitializer(
    private val uzumEventStreamAsyncLoop: UzumEventStreamAsyncLoop
) {
    @PostConstruct
    fun initialize() {
        uzumEventStreamAsyncLoop.startUzumDataStreamLoop()
//        uzumEventStreamAsyncLoop.startPaymentStreamLoop()
    }
}
