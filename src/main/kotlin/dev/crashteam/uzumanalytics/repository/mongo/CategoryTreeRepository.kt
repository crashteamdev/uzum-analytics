package dev.crashteam.uzumanalytics.repository.mongo

import dev.crashteam.uzumanalytics.domain.mongo.CategoryTreeDocument
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CategoryTreeRepository : ReactiveCrudRepository<CategoryTreeDocument, String> {
}
