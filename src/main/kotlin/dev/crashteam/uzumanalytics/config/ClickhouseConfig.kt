package dev.crashteam.uzumanalytics.config

import dev.crashteam.uzumanalytics.config.properties.ClickHouseDbProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.clickhouse.ClickHouseDataSource
import ru.yandex.clickhouse.settings.ClickHouseQueryParam
import java.util.*
import javax.sql.DataSource

@Configuration
class ClickhouseConfig {

    @Bean
    fun clickHouseDataSource(clickHouseDbProperties: ClickHouseDbProperties): ClickHouseDataSource {
        val info = Properties()
        info.setProperty(ClickHouseQueryParam.USER.key, clickHouseDbProperties.user)
        info.setProperty(ClickHouseQueryParam.PASSWORD.key, clickHouseDbProperties.password)
        info.setProperty(ClickHouseQueryParam.COMPRESS.key, clickHouseDbProperties.compress.toString())
        info.setProperty(
            ClickHouseQueryParam.CONNECT_TIMEOUT.key,
            clickHouseDbProperties.connectionTimeout.toString()
        )
        return ClickHouseDataSource(clickHouseDbProperties.url, info)
    }

    @Bean
    @Autowired
    fun clickHouseJdbcTemplate(clickHouseDataSource: DataSource): JdbcTemplate {
        return JdbcTemplate(clickHouseDataSource)
    }

}
