package org.sparkcorporation.lims.order.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.LabFileId;

@Converter(autoApply = false)
class LabFileIdConverter implements AttributeConverter<LabFileId, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable LabFileId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public @Nullable LabFileId convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null ? null : new LabFileId(dbData);
    }
}
