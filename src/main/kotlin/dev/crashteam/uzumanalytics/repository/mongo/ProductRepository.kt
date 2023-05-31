package dev.crashteam.uzumanalytics.repository.mongo

import dev.crashteam.uzumanalytics.mongo.ProductDocument
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface ProductRepository : ReactiveMongoRepository<ProductDocument, String>, ProductCustomRepository
