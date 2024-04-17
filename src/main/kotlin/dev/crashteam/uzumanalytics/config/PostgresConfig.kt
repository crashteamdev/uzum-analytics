package dev.crashteam.uzumanalytics.config

import liquibase.integration.spring.SpringLiquibase
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class PostgresConfig {

    @Bean
    @ConfigurationProperties(prefix = "postgresql.liquibase")
    fun postgresLiquibaseProperties(): LiquibaseProperties {
        return LiquibaseProperties()
    }

    @Bean
    fun postgresLiquibase(
        pgDataSource: DataSource,
        @Qualifier("postgresLiquibaseProperties") postgresLiquibaseProperties: LiquibaseProperties
    ): SpringLiquibase {
        return springLiquibase(pgDataSource, postgresLiquibaseProperties)
    }

    private fun springLiquibase(dataSource: DataSource, properties: LiquibaseProperties): SpringLiquibase {
        val liquibase = SpringLiquibase()
        liquibase.dataSource = dataSource
        liquibase.changeLog = properties.changeLog
        liquibase.contexts = properties.contexts
        liquibase.defaultSchema = properties.defaultSchema
        liquibase.isDropFirst = properties.isDropFirst
        liquibase.setShouldRun(properties.isEnabled)
        liquibase.labels = properties.labels
        liquibase.setChangeLogParameters(properties.parameters)
        liquibase.setRollbackFile(properties.rollbackFile)
        return liquibase
    }
}
