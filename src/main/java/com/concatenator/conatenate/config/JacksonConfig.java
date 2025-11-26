package com.concatenator.conatenate.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson Configuration
 *
 * Creates ObjectMapper bean for JSON serialization/deserialization.
 * This is needed because Spring Boot 4.0 uses tools.jackson by default.
 */
@Configuration
public class JacksonConfig {

    /**
     * Create and configure ObjectMapper bean.
     *
     * @return Configured ObjectMapper instance
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Enable pretty printing for readable JSON files
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Don't fail on unknown properties when deserializing
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return mapper;
    }
}
