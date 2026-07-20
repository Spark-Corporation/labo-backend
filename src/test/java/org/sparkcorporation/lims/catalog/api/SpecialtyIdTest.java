package org.sparkcorporation.lims.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.shared.id.Ulid;

class SpecialtyIdTest {

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> SpecialtyId.of(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_invalid_ulid() {
        assertThatThrownBy(() -> SpecialtyId.of("not-a-ulid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void accepts_valid_ulid() {
        String id = Ulid.generate();
        assertThat(SpecialtyId.of(id).value()).isEqualTo(id);
    }

    @Test
    void generate_returns_distinct_ids() {
        assertThat(SpecialtyId.generate()).isNotEqualTo(SpecialtyId.generate());
    }
}
