package dev.crashteam.uzumanalytics.controller.converter

import org.springframework.core.convert.converter.Converter

interface DataConverter<S, T> : Converter<S, T>
