package org.sparkcorporation.lims.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.shared.id.Ulid;

class ReferenceIntervalIdTest {

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> ReferenceIntervalId.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_invalid_ulid() {
        assertThatThrownBy(() -> ReferenceIntervalId.of("not-a-ulid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void accepts_valid_ulid_round_trip() {
        String id = Ulid.generate();
        assertThat(ReferenceIntervalId.of(id).value()).isEqualTo(id);
    }

    @Test
    void generate_returns_distinct_ids() {
        assertThat(ReferenceIntervalId.generate()).isNotEqualTo(ReferenceIntervalId.generate());
    }
}
