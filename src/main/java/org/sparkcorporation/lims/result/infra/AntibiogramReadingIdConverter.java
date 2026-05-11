package org.sparkcorporation.lims.result.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.result.api.AntibiogramReadingId;

/**
 * JPA {@link AttributeConverter} bridging the typed {@link AntibiogramReadingId} record to the
 * {@code VARCHAR(26)} ULID column. Mirrors {@code order::infra.LabFileIdConverter}.
 */
@Converter(autoApply = false)
class AntibiogramReadingIdConverter implements AttributeConverter<AntibiogramReadingId, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable AntibiogramReadingId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public @Nullable AntibiogramReadingId convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null ? null : new AntibiogramReadingId(dbData);
    }
}
