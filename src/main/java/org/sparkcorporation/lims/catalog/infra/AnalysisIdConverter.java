package org.sparkcorporation.lims.catalog.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.AnalysisId;

@Converter(autoApply = false)
class AnalysisIdConverter implements AttributeConverter<AnalysisId, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable AnalysisId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public @Nullable AnalysisId convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null ? null : new AnalysisId(dbData);
    }
}
