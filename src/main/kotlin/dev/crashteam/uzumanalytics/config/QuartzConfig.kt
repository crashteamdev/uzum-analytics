package dev.crashteam.uzumanalytics.config

import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.quartz.QuartzProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.quartz.SchedulerFactoryBean
import java.util.Properties
import javax.sql.DataSource

@Configuration
@ConditionalOnProperty(
    value = ["uzum.scheduleEnabled"],
    havingValue = "true",
    matchIfMissing = true
)
class QuartzConfig {

    @Autowired
    private lateinit var quartzProperties: QuartzProperties

    fun getAllProperties(): Properties {
        val props = Properties()
        props.putAll(quartzProperties.properties)
        return props
    }

    @Bean
    fun schedulerFactoryBean(schedulerFactoryBean: SchedulerFactoryBean): SchedulerFactoryBean {
        schedulerFactoryBean.setApplicationContextSchedulerContextKey("applicationContext")
        return schedulerFactoryBean
    }
}
