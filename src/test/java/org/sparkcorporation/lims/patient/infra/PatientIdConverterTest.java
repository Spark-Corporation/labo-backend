package org.sparkcorporation.lims.patient.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.patient.api.PatientId;

class PatientIdConverterTest {

    private final PatientIdConverter converter = new PatientIdConverter();

    @Test
    void convertToDatabaseColumn_extracts_value() {
        var id = PatientId.next();
        assertThat(converter.convertToDatabaseColumn(id)).isEqualTo(id.value());
    }

    @Test
    void convertToDatabaseColumn_null_in_null_out() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void convertToEntityAttribute_wraps_value() {
        var id = PatientId.next();
        assertThat(converter.convertToEntityAttribute(id.value())).isEqualTo(id);
    }

    @Test
    void convertToEntityAttribute_null_in_null_out() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
