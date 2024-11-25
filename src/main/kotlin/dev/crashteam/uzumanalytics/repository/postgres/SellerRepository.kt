package dev.crashteam.uzumanalytics.repository.postgres

import dev.crashteam.uzumanalytics.db.model.tables.pojos.Sellers

interface SellerRepository {
    fun save(seller: Sellers)

    fun saveBatch(sellers: Collection<Sellers>): IntArray

    fun findBySellerLink(sellerLink: String): Sellers?

    fun findByAccountId(accountId: Long): List<Sellers>

    fun findAccountIdsBySellerLink(sellerLink: String): List<Long>
}
