package dev.crashteam.uzumanalytics.converter.view

import dev.crashteam.uzumanalytics.controller.model.CategoryDataView
import dev.crashteam.uzumanalytics.controller.model.CategorySalesSellerView
import dev.crashteam.uzumanalytics.controller.model.CategorySalesView
import dev.crashteam.uzumanalytics.controller.model.CategorySalesViewWrapper
import dev.crashteam.uzumanalytics.converter.DataConverter
import dev.crashteam.uzumanalytics.service.model.AggregateSalesWrapper
import org.springframework.stereotype.Component

@Component
class CategoryHistoryToDataConverter : DataConverter<AggregateSalesWrapper, CategorySalesViewWrapper> {

    override fun convert(source: AggregateSalesWrapper): CategorySalesViewWrapper {
        return CategorySalesViewWrapper().apply {
            data = source.data.map {
                CategorySalesView().apply {
                    productId = it.productId
                    skuId = it.skuId
                    name = it.name
                    seller = CategorySalesSellerView().apply {
                        id = it.seller.id
                        name = it.seller.name
                    }
                    category = CategoryDataView().apply {
                        name = it.category.name
                    }
                    availableAmount = it.availableAmount
                    price = it.price
                    proceeds = it.proceeds
                    priceGraph = it.priceGraph
                    orderGraph = it.orderGraph
                    daysInStock = it.daysInStock
                }
            }
            pageSize = source.meta.pageSize
            page = source.meta.page
            totalPages = source.meta.pages
        }
    }
}
