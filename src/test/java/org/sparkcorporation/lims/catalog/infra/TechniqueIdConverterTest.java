package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.TechniqueId;

class TechniqueIdConverterTest {

    private final TechniqueIdConverter conv = new TechniqueIdConverter();

    @Test
    void convertToDatabaseColumn_extracts_value() {
        var id = TechniqueId.next();
        assertThat(conv.convertToDatabaseColumn(id)).isEqualTo(id.value());
    }

    @Test
    void convertToDatabaseColumn_null_in_null_out() {
        assertThat(conv.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void convertToEntityAttribute_wraps_value() {
        var id = TechniqueId.next();
        assertThat(conv.convertToEntityAttribute(id.value())).isEqualTo(id);
    }

    @Test
    void convertToEntityAttribute_null_in_null_out() {
        assertThat(conv.convertToEntityAttribute(null)).isNull();
    }
}
