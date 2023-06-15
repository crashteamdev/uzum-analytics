package dev.crashteam.uzumanalytics.config

import dev.crashteam.uzumanalytics.controller.converter.DataConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.support.ConversionServiceFactoryBean

@Configuration
@ComponentScan(
    basePackages = [
        "dev.crashteam.uzumanalytics.controller.converter",
        "dev.crashteam.uzumanalytics.domain.mongo.converter"
    ]
)
class ConverterConfig {

    @Bean
    @Primary
    fun conversionServiceFactoryBean(converters: Set<DataConverter<*, *>>): ConversionServiceFactoryBean {
        val conversionServiceFactoryBean = ConversionServiceFactoryBean()
        conversionServiceFactoryBean.setConverters(converters)

        return conversionServiceFactoryBean
    }
}
