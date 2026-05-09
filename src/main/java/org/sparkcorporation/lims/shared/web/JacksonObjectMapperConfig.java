package org.sparkcorporation.lims.shared.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4 auto-configures the Jackson 3 (tools.jackson) {@code JsonMapper} but no longer
 * exposes a Jackson 2 {@code com.fasterxml.jackson.databind.ObjectMapper}. Several controllers use
 * {@code ObjectMapper#convertValue(Map, Class)} for PATCH map-binding; this config restores the
 * Jackson 2 bean until those call-sites migrate to Jackson 3.
 */
@Configuration
class JacksonObjectMapperConfig {

    @Bean
    ObjectMapper jackson2ObjectMapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
