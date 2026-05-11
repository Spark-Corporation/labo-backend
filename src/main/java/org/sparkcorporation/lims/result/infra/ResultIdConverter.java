package org.sparkcorporation.lims.result.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.result.api.ResultId;

/**
 * JPA {@link AttributeConverter} bridging the typed {@link ResultId} record to the {@code
 * VARCHAR(26)} ULID column. Mirrors {@code order::infra.LabFileIdConverter}; {@code
 * autoApply=false} so each field opts in via {@code @Convert}.
 */
@Converter(autoApply = false)
class ResultIdConverter implements AttributeConverter<ResultId, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable ResultId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public @Nullable ResultId convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null ? null : new ResultId(dbData);
    }
}
