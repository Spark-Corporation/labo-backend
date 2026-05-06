package org.sparkcorporation.lims.catalog.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.DisciplineId;

@Converter(autoApply = false)
public class DisciplineIdConverter implements AttributeConverter<DisciplineId, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable DisciplineId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public @Nullable DisciplineId convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null ? null : new DisciplineId(dbData);
    }
}
