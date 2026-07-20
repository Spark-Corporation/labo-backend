package org.sparkcorporation.lims.patient.infra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.patient.api.PatientId;

/**
 * Converts {@link PatientId} ↔ {@code String} at the JPA boundary.
 *
 * <p>{@code autoApply = false}: this converter is NOT applied to {@code Patient.id} itself (the
 * entity stores raw {@code String}); it is intended for child entities that hold a {@code
 * PatientId} field via {@code @Convert(converter = PatientIdConverter.class)}.
 */
@Converter(autoApply = false)
public class PatientIdConverter implements AttributeConverter<PatientId, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable PatientId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public @Nullable PatientId convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null ? null : new PatientId(dbData);
    }
}
