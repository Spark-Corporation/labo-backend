package org.sparkcorporation.lims.catalog.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.PreleveurId;

@Converter(autoApply = false)
class PreleveurIdConverter implements AttributeConverter<PreleveurId, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable PreleveurId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public @Nullable PreleveurId convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null ? null : new PreleveurId(dbData);
    }
}
