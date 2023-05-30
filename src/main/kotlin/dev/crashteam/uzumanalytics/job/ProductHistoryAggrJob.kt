//package dev.crashteam.uzumanalytics.job
//
//import com.mongodb.BasicDBObject
//import kotlinx.coroutines.reactive.awaitLast
//import kotlinx.coroutines.reactor.awaitSingleOrNull
//import kotlinx.coroutines.runBlocking
//import dev.crashteam.uzumanalytics.domain.mongo.PRODUCT_HISTORY_AGGR_COLLECTION_NAME
//import dev.crashteam.uzumanalytics.domain.mongo.ProductAggrHistoryDocument
//import dev.crashteam.uzumanalytics.domain.mongo.ProductHistoryDocument
//import dev.crashteam.uzumanalytics.extensions.getApplicationContext
//import org.bson.Document
//import org.quartz.DisallowConcurrentExecution
//import org.quartz.Job
//import org.quartz.JobExecutionContext
//import org.springframework.context.ApplicationContext
//import org.springframework.data.domain.Sort
//import org.springframework.data.mongodb.core.ReactiveMongoTemplate
//import org.springframework.data.mongodb.core.aggregation.Aggregation
//import org.springframework.data.mongodb.core.aggregation.AggregationOptions
//import org.springframework.data.mongodb.core.aggregation.ConvertOperators
//import org.springframework.data.mongodb.core.aggregation.Fields
//import org.springframework.data.mongodb.core.aggregation.TypedAggregation
//import org.springframework.data.mongodb.core.index.CompoundIndexDefinition
//import org.springframework.data.mongodb.core.index.Index
//import org.springframework.data.mongodb.core.index.IndexDefinition
//import org.springframework.data.mongodb.core.query.Criteria
//import java.time.ZoneOffset
//import java.util.Date
//
//@DisallowConcurrentExecution
//class ProductHistoryAggrJob : Job {
//
//    override fun execute(context: JobExecutionContext) {
//        val applicationContext = context.getApplicationContext()
//        aggregateProductHistory(applicationContext)
//    }
//
//    private fun aggregateProductHistory(applicationContext: ApplicationContext) {
//        val where = Criteria.where("productId").ne(null)
//        val match = Aggregation.match(where)
//        val unwind = Aggregation.unwind("change.split")
//        val project = Aggregation.project("productId")
//            .and("updatedAt").`as`("date")
//            .and("change.parentCategory").`as`("parentCategory")
//            .and("change.ancestorCategories").`as`("ancestorCategories")
//            .and("change.reviewsAmount").`as`("reviewsAmount")
//            .and("change.orderAmount").`as`("totalOrderAmount")
//            .and("change.totalAvailableAmount").`as`("totalAvailableAmount")
//            .and("change.split._id").`as`("skuId")
//            .and("change.title").`as`("name")
//            .and("change.seller.id").`as`("sellerId")
//            .and("change.seller.sellerAccountId").`as`("sellerAccountId")
//            .and("change.seller.title").`as`("sellerName")
//            .and("change.split.characteristics").`as`("skuCharacteristics")
//            .and("change.split.availableAmount").`as`("skuAvailableAmount")
//            .and(ConvertOperators.valueOf("change.split.purchasePrice").convertToDecimal()).`as`("price")
//        val groupOperation = Aggregation.group(Fields.fields("productId", "skuId"))
//            .push(
//                BasicDBObject("date", "\$date")
//                    .append("totalOrderAmount", "\$totalOrderAmount")
//                    .append("totalAvailableAmount", "\$totalAvailableAmount")
//                    .append("name", "\$name")
//                    .append(
//                        "seller",
//                        BasicDBObject("title", "\$sellerName")
//                            .append("id", "\$sellerId")
//                            .append("accountId", "\$sellerAccountId")
//                    )
//                    .append("reviewsAmount", "\$reviewsAmount")
//                    .append("parentCategory", "\$parentCategory")
//                    .append("ancestorCategories", "\$ancestorCategories")
//                    .append("skuCharacteristics", "\$skuCharacteristics")
//                    .append("skuAvailableAmount", "\$skuAvailableAmount")
//                    .append("price", "\$price")
//            ).`as`("skuChange")
//        val options = AggregationOptions.builder().allowDiskUse(true).build()
//        val out = Aggregation.out(PRODUCT_HISTORY_AGGR_COLLECTION_NAME)
//        val aggr: TypedAggregation<ProductHistoryDocument> =
//            Aggregation.newAggregation(
//                ProductHistoryDocument::class.java,
//                listOf(match, unwind, project, groupOperation, out)
//            ).withOptions(options)
//        runBlocking {
//            val mongoTemplate = applicationContext.getBean(ReactiveMongoTemplate::class.java)
//            val productHistoryAggrCollection =
//                mongoTemplate.getCollection(PRODUCT_HISTORY_AGGR_COLLECTION_NAME).awaitSingleOrNull()
//            if (productHistoryAggrCollection == null) {
//                mongoTemplate.createCollection(ProductAggrHistoryDocument::class.java).awaitSingleOrNull()
//            }
//            val productIdSkuIdIndex: IndexDefinition =
//                CompoundIndexDefinition(
//                    Document().append("id.productId", 1).append("id.skuId", 1)
//                ).unique()
//            mongoTemplate.indexOps(ProductAggrHistoryDocument::class.java).ensureIndex(productIdSkuIdIndex).awaitSingleOrNull()
//            val ancestorCategoriesIndex: IndexDefinition =
//                CompoundIndexDefinition(
//                    Document().append("skuChange.ancestorCategories", 1).append("skuChange.date", 1)
//                )
//            mongoTemplate.indexOps(ProductAggrHistoryDocument::class.java).ensureIndex(ancestorCategoriesIndex).awaitSingleOrNull()
//            mongoTemplate.indexOps(ProductAggrHistoryDocument::class.java)
//                .ensureIndex(Index().on("skuChange.date", Sort.Direction.ASC)).awaitSingleOrNull()
//            mongoTemplate.indexOps(ProductAggrHistoryDocument::class.java)
//                .ensureIndex(Index().on("id.skuId", Sort.Direction.ASC)).awaitSingleOrNull()
//
//            mongoTemplate.aggregate(aggr, ProductAggrHistoryDocument::class.java).awaitLast()
//        }
//    }
//}
