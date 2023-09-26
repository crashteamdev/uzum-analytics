package dev.crashteam.uzumanalytics.stream.listener.aws

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory
import dev.crashteam.uzumanalytics.stream.handler.UzumScrapEventHandler
import org.springframework.stereotype.Component

@Component
class UzumEventStreamProcessorFactory(
    private val uzumScrapEventHandlers: List<UzumScrapEventHandler>,
) : IRecordProcessorFactory {
    override fun createProcessor(): IRecordProcessor {
        return UzumEventStreamListener(uzumScrapEventHandlers)
    }
}
