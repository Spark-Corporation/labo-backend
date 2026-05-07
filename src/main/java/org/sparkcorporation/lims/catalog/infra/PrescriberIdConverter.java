package org.sparkcorporation.lims.catalog.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.PrescriberId;

@Converter(autoApply = false)
class PrescriberIdConverter implements AttributeConverter<PrescriberId, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable PrescriberId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public @Nullable PrescriberId convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null ? null : new PrescriberId(dbData);
    }
}
