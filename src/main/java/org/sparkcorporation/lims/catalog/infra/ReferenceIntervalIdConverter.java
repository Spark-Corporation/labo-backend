package org.sparkcorporation.lims.catalog.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.ReferenceIntervalId;

@Converter(autoApply = false)
class ReferenceIntervalIdConverter implements AttributeConverter<ReferenceIntervalId, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable ReferenceIntervalId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public @Nullable ReferenceIntervalId convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null ? null : new ReferenceIntervalId(dbData);
    }
}
