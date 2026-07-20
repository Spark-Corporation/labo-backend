package org.sparkcorporation.lims.catalog.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.AntibioticId;

@Converter(autoApply = false)
public class AntibioticIdConverter implements AttributeConverter<AntibioticId, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable AntibioticId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public @Nullable AntibioticId convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null ? null : new AntibioticId(dbData);
    }
}
