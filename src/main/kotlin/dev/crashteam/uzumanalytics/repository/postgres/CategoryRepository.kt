package dev.crashteam.uzumanalytics.repository.postgres

import dev.crashteam.uzumanalytics.db.model.tables.pojos.CategoryHierarchical

interface CategoryRepository {

    fun save(categoryHierarchical: CategoryHierarchical)

}
