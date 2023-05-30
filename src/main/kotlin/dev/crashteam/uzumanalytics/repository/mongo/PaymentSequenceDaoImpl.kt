package dev.crashteam.uzumanalytics.repository.mongo

import dev.crashteam.uzumanalytics.repository.mongo.model.SequenceId
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.stereotype.Component

@Component
class PaymentSequenceDaoImpl(
    private val reactiveMongoOperations: ReactiveMongoOperations
) : PaymentSequenceDao {

    override suspend fun getNextSequenceId(key: String): Long {
        val query = Query(Criteria.where("_id").`is`(key))
        val update = Update()
        update.inc("seq", 1)
        val options = FindAndModifyOptions()
        options.returnNew(true)
        val seqId: SequenceId =
            reactiveMongoOperations.findAndModify(query, update, options, SequenceId::class.java).awaitSingle()

        return seqId.seq
    }
}
