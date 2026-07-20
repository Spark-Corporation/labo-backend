package org.sparkcorporation.lims.result.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.result.api.ResultLineId;

/**
 * JPA {@link AttributeConverter} bridging the typed {@link ResultLineId} record to the {@code
 * VARCHAR(26)} ULID column. Mirrors {@code order::infra.LabFileIdConverter}.
 */
@Converter(autoApply = false)
class ResultLineIdConverter implements AttributeConverter<ResultLineId, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable ResultLineId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public @Nullable ResultLineId convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null ? null : new ResultLineId(dbData);
    }
}
