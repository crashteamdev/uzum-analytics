package dev.crashteam.uzumanalytics.stream.handler.analytics

import dev.crashteam.uzum.scrapper.data.v1.UzumScrapperEvent

interface UzumScrapEventHandler {

    fun handle(events: List<UzumScrapperEvent>)

    fun isHandle(event: UzumScrapperEvent): Boolean
}
