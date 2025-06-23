package com.bank.userservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурационный класс для настройки Jackson ObjectMapper.
 * Настраивает сериализацию дат и форматирование вывода JSON.
 */
@Configuration
public class JacksonConfig {

    /**
     * Создает и настраивает ObjectMapper для корректной работы с датами и JSON.
     *
     * @return настроенный экземпляр ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);  // Красивое форматирование JSON
        mapper.registerModule(new JavaTimeModule());        // Поддержка Java 8 Time API
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Использовать строковое представление дат
        return mapper;
    }
}
