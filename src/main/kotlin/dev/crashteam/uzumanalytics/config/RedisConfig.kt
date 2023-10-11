package dev.crashteam.uzumanalytics.config

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.crashteam.uzumanalytics.client.cbr.model.CbrCurrencyRateResponse
import dev.crashteam.uzumanalytics.client.currencyapi.model.CurrencyApiResponse
import dev.crashteam.uzumanalytics.config.properties.RedisProperties
import dev.crashteam.uzumanalytics.repository.clickhouse.model.ChCategoryOverallInfo
import dev.crashteam.uzumanalytics.repository.redis.ApiKeyUserSessionInfo
import dev.crashteam.uzumanalytics.service.model.SellerOverallInfo
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
                .serializeValuesWith(redisJsonSerializer(CurrencyApiResponse::class.java))
                .entryTtl(Duration.ofSeconds(86400))
            configurationMap[CATEGORY_OVERALL_INFO_CACHE] = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(redisJsonSerializer(ChCategoryOverallInfo::class.java))
                .entryTtl(Duration.ofSeconds(21600))
            configurationMap[SELLER_OVERALL_INFO_CACHE_NAME] = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(redisJsonSerializer(SellerOverallInfo::class.java))
                .entryTtl(Duration.ofSeconds(21600))
            builder.withInitialCacheConfigurations(configurationMap)
        }
    }

    private inline fun <reified T> redisJsonSerializer(
        valueClass: Class<T>
    ): RedisSerializationContext.SerializationPair<Any> {
        val objectMapper = jacksonObjectMapper().registerModules(JavaTimeModule())
        return RedisSerializationContext.SerializationPair.fromSerializer(object :
            RedisSerializer<Any> {
            override fun serialize(t: Any?): ByteArray {
                return objectMapper.writeValueAsBytes(t)
            }

            override fun deserialize(bytes: ByteArray?): Any? {
                return if (bytes != null) {
                    objectMapper.readValue(bytes, valueClass)
                } else null
            }

        })
    }

    companion object {
        const val UZUM_CBR_CURRENCIES_CACHE_NAME = "cbr-currencies"
        const val CURRENCY_API_CACHE_NAME = "currency-api"
        const val CATEGORY_OVERALL_INFO_CACHE = "category-overall-info"
        const val SELLER_OVERALL_INFO_CACHE_NAME = "seller-overall-info"
    }

}
