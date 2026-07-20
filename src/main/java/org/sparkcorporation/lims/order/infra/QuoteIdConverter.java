package org.sparkcorporation.lims.order.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.QuoteId;

@Converter(autoApply = false)
class QuoteIdConverter implements AttributeConverter<QuoteId, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable QuoteId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public @Nullable QuoteId convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null ? null : new QuoteId(dbData);
    }
}
