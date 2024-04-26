package dev.crashteam.uzumanalytics.repository.postgres

import dev.crashteam.uzumanalytics.db.model.tables.CategoryHierarchical.CATEGORY_HIERARCHICAL
import dev.crashteam.uzumanalytics.db.model.tables.pojos.CategoryHierarchical
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class CategoryJooqRepository(
    private val dsl: DSLContext
) : CategoryRepository {

    override fun save(categoryHierarchical: CategoryHierarchical) {
        val ch = CATEGORY_HIERARCHICAL
        dsl.insertInto(ch, ch.CATEGORY_ID, ch.PARENT_CATEGORY_ID, ch.TITLE)
            .values(categoryHierarchical.categoryId, categoryHierarchical.parentCategoryId, categoryHierarchical.title)
            .onConflict(ch.CATEGORY_ID)
            .doUpdate().set(
                mapOf(
                    ch.PARENT_CATEGORY_ID to categoryHierarchical.parentCategoryId,
                    ch.TITLE to categoryHierarchical.title,
                )
            ).execute()
    }
}
