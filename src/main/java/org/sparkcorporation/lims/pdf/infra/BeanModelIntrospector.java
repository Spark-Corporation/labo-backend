package org.sparkcorporation.lims.pdf.infra;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

final class BeanModelIntrospector {

    private static final ObjectMapper MAPPER =
            new ObjectMapper()
                    .findAndRegisterModules()
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private BeanModelIntrospector() {}

    static <T> Map<String, Object> toMap(T model) {
        if (model == null) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(MAPPER.convertValue(model, MAP_TYPE));
    }
}
