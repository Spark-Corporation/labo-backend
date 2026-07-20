package org.sparkcorporation.lims.order.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.QuoteLineId;

@Converter(autoApply = false)
class QuoteLineIdConverter implements AttributeConverter<QuoteLineId, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable QuoteLineId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public @Nullable QuoteLineId convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null ? null : new QuoteLineId(dbData);
    }
}
