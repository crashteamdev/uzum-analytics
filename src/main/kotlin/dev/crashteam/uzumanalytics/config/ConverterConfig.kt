package dev.crashteam.uzumanalytics.config

import dev.crashteam.uzumanalytics.controller.converter.ViewConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.support.ConversionServiceFactoryBean
import org.springframework.core.convert.converter.Converter

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
    fun conversionServiceFactoryBean(converters: Set<ViewConverter<*, *>>): ConversionServiceFactoryBean {
        val conversionServiceFactoryBean = ConversionServiceFactoryBean()
        conversionServiceFactoryBean.setConverters(converters)

        return conversionServiceFactoryBean
    }
}
