package org.sparkcorporation.lims.order.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.AnalysisRequestId;

@Converter(autoApply = false)
class AnalysisRequestIdConverter implements AttributeConverter<AnalysisRequestId, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable AnalysisRequestId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public @Nullable AnalysisRequestId convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null ? null : new AnalysisRequestId(dbData);
    }
}
