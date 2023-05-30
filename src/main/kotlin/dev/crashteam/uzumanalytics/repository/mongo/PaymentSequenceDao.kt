package dev.crashteam.uzumanalytics.repository.mongo

interface PaymentSequenceDao {
    suspend fun getNextSequenceId(key: String): Long
}
