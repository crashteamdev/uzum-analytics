package dev.crashteam.uzumanalytics.config

import dev.crashteam.uzumanalytics.domain.mongo.ProductPositionTSDocument
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition
import javax.annotation.PostConstruct

@Configuration
class MongoConfig(
    private val reactiveMongoTemplate: ReactiveMongoTemplate
) {

    @PostConstruct
    fun init() = runBlocking {
        val collectionExists =
            reactiveMongoTemplate.collectionExists(ProductPositionTSDocument::class.java).awaitSingle()
        if (!collectionExists) {
            reactiveMongoTemplate.createCollection(ProductPositionTSDocument::class.java).awaitSingle()
            reactiveMongoTemplate.indexOps(ProductPositionTSDocument::class.java)
                .ensureIndex(CompoundIndexDefinition(
                    Document().append("metadata.id.productId", 1)
                        .append("metadata.id.skuId", 1)
                        .append("metadata.categoryId", 1)
                )).awaitSingle()
        }
    }
}
