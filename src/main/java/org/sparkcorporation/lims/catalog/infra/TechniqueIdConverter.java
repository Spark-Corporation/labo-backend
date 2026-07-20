package org.sparkcorporation.lims.catalog.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.TechniqueId;

@Converter(autoApply = false)
public class TechniqueIdConverter implements AttributeConverter<TechniqueId, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable TechniqueId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public @Nullable TechniqueId convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null ? null : new TechniqueId(dbData);
    }
}
