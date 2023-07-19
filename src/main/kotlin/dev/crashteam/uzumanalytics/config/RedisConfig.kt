package dev.crashteam.uzumanalytics.config

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.crashteam.uzumanalytics.client.cbr.model.CbrCurrencyRateResponse
import dev.crashteam.uzumanalytics.client.currencyapi.model.CurrencyApiResponse
import dev.crashteam.uzumanalytics.config.properties.RedisProperties
import dev.crashteam.uzumanalytics.repository.clickhouse.model.ChCategoryOverallInfo
import dev.crashteam.uzumanalytics.repository.redis.ApiKeyUserSessionInfo
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.RedisSystemException
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.LettuceClientConfigurationBuilder
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.*
import org.springframework.data.redis.stream.StreamReceiver
import java.nio.ByteBuffer
import java.time.Duration

private val log = KotlinLogging.logger {}

@Configuration
class RedisConfig(
    private val xmlMapper: XmlMapper,
    private val redisProperties: RedisProperties,
) {

    @Bean
    fun reactiveRedisTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, Long> {
        val jdkSerializationRedisSerializer = JdkSerializationRedisSerializer()
        val stringRedisSerializer = StringRedisSerializer.UTF_8
        val longToStringSerializer = GenericToStringSerializer(Long::class.java)
        return ReactiveRedisTemplate(
            factory,
            RedisSerializationContext.newSerializationContext<String, Long>(jdkSerializationRedisSerializer)
                .key(stringRedisSerializer).value(longToStringSerializer).build()
        )
    }

    @Bean
    fun apiKeySessionRedisTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, ApiKeyUserSessionInfo> {
        val jdkSerializationRedisSerializer = JdkSerializationRedisSerializer()
        val stringRedisSerializer = StringRedisSerializer.UTF_8
        val jackson2JsonRedisSerializer = Jackson2JsonRedisSerializer(ApiKeyUserSessionInfo::class.java)
        return ReactiveRedisTemplate(
            factory,
            RedisSerializationContext.newSerializationContext<String, ApiKeyUserSessionInfo>(
                jdkSerializationRedisSerializer
            )
                .key(stringRedisSerializer).value(jackson2JsonRedisSerializer).build()
        )
    }

    @Bean
    fun builderCustomizer(): LettuceClientConfigurationBuilderCustomizer {
        return LettuceClientConfigurationBuilderCustomizer { builder: LettuceClientConfigurationBuilder ->
            builder.useSsl().disablePeerVerification()
        }
    }

    @Bean
    fun uzumProductSubscription(
        redisConnectionFactory: ReactiveRedisConnectionFactory
    ): StreamReceiver<String, ObjectRecord<String, String>> {
        val options = StreamReceiver.StreamReceiverOptions.builder().pollTimeout(Duration.ofMillis(100))
            .targetType(String::class.java).build()
        try {
//            redisConnectionFactory.reactiveConnection.streamCommands().xGroupDestroy(
//                ByteBuffer.wrap(redisProperties.stream.keProductInfo.streamName.toByteArray()),
//                redisProperties.stream.keProductInfo.consumerGroup
//            )?.subscribe()
            redisConnectionFactory.reactiveConnection.streamCommands().xGroupCreate(
                ByteBuffer.wrap(redisProperties.stream.keProductInfo.streamName.toByteArray()),
                redisProperties.stream.keProductInfo.consumerGroup,
                ReadOffset.from("0-0"),
                true
            ).subscribe()
        } catch (e: RedisSystemException) {
            log.warn(e) { "Failed to create consumer group: ${redisProperties.stream.keProductInfo.consumerGroup}" }
        }
        return StreamReceiver.create(redisConnectionFactory, options)
    }

    @Bean
    fun uzumProductPositionSubscription(
        redisConnectionFactory: ReactiveRedisConnectionFactory
    ): StreamReceiver<String, ObjectRecord<String, String>> {
        val options = StreamReceiver.StreamReceiverOptions.builder().pollTimeout(Duration.ofMillis(100))
            .targetType(String::class.java).build()
        try {
//            redisConnectionFactory.reactiveConnection.streamCommands().xGroupDestroy(
//                ByteBuffer.wrap(redisProperties.stream.keProductPosition.streamName.toByteArray()),
//                redisProperties.stream.keProductPosition.consumerGroup
//            )?.subscribe()
            redisConnectionFactory.reactiveConnection.streamCommands().xGroupCreate(
                ByteBuffer.wrap(redisProperties.stream.keProductPosition.streamName.toByteArray()),
                redisProperties.stream.keProductPosition.consumerGroup,
                ReadOffset.from("0-0"),
                true
            ).subscribe()
        } catch (e: RedisSystemException) {
            log.warn(e) { "Failed to create consumer group: ${redisProperties.stream.keProductPosition.consumerGroup}" }
        }
        return StreamReceiver.create(redisConnectionFactory, options)
    }

    @Bean
    fun uzumCategorySubscription(
        redisConnectionFactory: ReactiveRedisConnectionFactory
    ): StreamReceiver<String, ObjectRecord<String, String>> {
        val options = StreamReceiver.StreamReceiverOptions.builder().pollTimeout(Duration.ofMillis(100))
            .targetType(String::class.java).build()
        try {
//            redisConnectionFactory.reactiveConnection.streamCommands().xGroupDestroy(
//                ByteBuffer.wrap(redisProperties.stream.keCategoryInfo.streamName.toByteArray()),
//                redisProperties.stream.keCategoryInfo.consumerGroup
//            )?.subscribe()
            redisConnectionFactory.reactiveConnection.streamCommands().xGroupCreate(
                ByteBuffer.wrap(redisProperties.stream.keCategoryInfo.streamName.toByteArray()),
                redisProperties.stream.keCategoryInfo.consumerGroup,
                ReadOffset.from("0-0"),
                true
            ).subscribe()
        } catch (e: RedisSystemException) {
            log.warn(e) { "Failed to create consumer group: ${redisProperties.stream.keCategoryInfo.consumerGroup}" }
        }
        return StreamReceiver.create(redisConnectionFactory, options)
    }

    @Bean
    fun redisCacheManagerBuilderCustomizer(): RedisCacheManagerBuilderCustomizer {
        return RedisCacheManagerBuilderCustomizer { builder: RedisCacheManager.RedisCacheManagerBuilder ->
            val configurationMap: MutableMap<String, RedisCacheConfiguration> =
                HashMap()
            configurationMap[UZUM_CBR_CURRENCIES_CACHE_NAME] = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(object :
                    RedisSerializer<Any> {
                    override fun serialize(t: Any?): ByteArray {
                        return xmlMapper.writeValueAsBytes(t)
                    }

                    override fun deserialize(bytes: ByteArray?): Any? {
                        return if (bytes != null) {
                            xmlMapper.readValue(bytes, CbrCurrencyRateResponse::class.java)
                        } else null
                    }

                }))
                .entryTtl(Duration.ofSeconds(86400))
            configurationMap[CURRENCY_API_CACHE_NAME] = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(object :
                    RedisSerializer<Any> {
                    override fun serialize(t: Any?): ByteArray {
                        return jacksonObjectMapper().writeValueAsBytes(t)
                    }

                    override fun deserialize(bytes: ByteArray?): Any? {
                        return if (bytes != null) {
                            jacksonObjectMapper().readValue(bytes, CurrencyApiResponse::class.java)
                        } else null
                    }

                }))
                .entryTtl(Duration.ofSeconds(86400))
            configurationMap[CATEGORY_OVERALL_INFO_CACHE] = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(object :
                    RedisSerializer<Any> {
                    override fun serialize(t: Any?): ByteArray {
                        return jacksonObjectMapper().writeValueAsBytes(t)
                    }

                    override fun deserialize(bytes: ByteArray?): Any? {
                        return if (bytes != null) {
                            jacksonObjectMapper().readValue(bytes, ChCategoryOverallInfo::class.java)
                        } else null
                    }

                }))
                .entryTtl(Duration.ofSeconds(86400))
            builder.withInitialCacheConfigurations(configurationMap)
        }
    }

    companion object {
        const val UZUM_CBR_CURRENCIES_CACHE_NAME = "cbr-currencies"
        const val CURRENCY_API_CACHE_NAME = "currency-api"
        const val CATEGORY_OVERALL_INFO_CACHE = "category-overall-info"
    }

}
