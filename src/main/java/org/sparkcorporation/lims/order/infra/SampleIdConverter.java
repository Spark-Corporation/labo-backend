package org.sparkcorporation.lims.order.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.SampleId;

@Converter(autoApply = false)
class SampleIdConverter implements AttributeConverter<SampleId, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable SampleId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public @Nullable SampleId convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null ? null : new SampleId(dbData);
    }
}
