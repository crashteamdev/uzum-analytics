package dev.crashteam.uzumanalytics.repository.mongo.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "sequence")
data class SequenceId(
    @Id
    val id: String,
    val seq: Long
)
