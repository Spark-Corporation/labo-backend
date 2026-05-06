package org.sparkcorporation.lims.catalog.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.RecipientId;

@Converter(autoApply = false)
public class RecipientIdConverter implements AttributeConverter<RecipientId, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable RecipientId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public @Nullable RecipientId convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null ? null : new RecipientId(dbData);
    }
}
