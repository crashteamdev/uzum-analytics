package dev.crashteam.uzumanalytics.config

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.crashteam.uzumanalytics.client.cbr.model.CbrCurrencyRateResponse
import dev.crashteam.uzumanalytics.client.currencyapi.model.CurrencyApiResponse
import dev.crashteam.uzumanalytics.repository.redis.ApiKeyUserSessionInfo
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.LettuceClientConfigurationBuilder
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.*
import java.time.Duration

@Configuration
class RedisConfig(
    private val xmlMapper: XmlMapper
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
            builder.withInitialCacheConfigurations(configurationMap)
        }
    }

    companion object {
        const val UZUM_CBR_CURRENCIES_CACHE_NAME = "cbr-currencies"
        const val CURRENCY_API_CACHE_NAME = "currency-api"
    }

}
