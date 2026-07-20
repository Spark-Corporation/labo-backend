package org.sparkcorporation.lims.catalog.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.MatrixId;

@Converter(autoApply = false)
public class MatrixIdConverter implements AttributeConverter<MatrixId, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable MatrixId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public @Nullable MatrixId convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null ? null : new MatrixId(dbData);
    }
}
