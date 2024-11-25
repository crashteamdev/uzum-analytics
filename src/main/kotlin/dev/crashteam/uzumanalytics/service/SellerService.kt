package dev.crashteam.uzumanalytics.service

import dev.crashteam.uzumanalytics.db.model.tables.pojos.Sellers
import dev.crashteam.uzumanalytics.repository.postgres.SellerRepository
import org.springframework.stereotype.Service

@Service
class SellerService(
    private val sellerRepository: SellerRepository
) {

    fun findSellersByLink(sellerLink: String): List<Sellers> {
        val accountIds = sellerRepository.findAccountIdsBySellerLink(sellerLink)
        return accountIds.flatMap {
            sellerRepository.findByAccountId(it)
        }
    }
}
