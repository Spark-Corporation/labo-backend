package org.sparkcorporation.lims.catalog.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.SpecialtyId;

@Converter(autoApply = false)
class SpecialtyIdConverter implements AttributeConverter<SpecialtyId, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable SpecialtyId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public @Nullable SpecialtyId convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null ? null : new SpecialtyId(dbData);
    }
}
