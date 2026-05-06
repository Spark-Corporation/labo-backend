package org.sparkcorporation.lims.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.shared.id.Ulid;

class AntibioticIdTest {

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new AntibioticId(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_invalid_ulid() {
        assertThatThrownBy(() -> new AntibioticId("not-a-ulid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid ULID");
    }

    @Test
    void accepts_valid_ulid() {
        String id = Ulid.generate();
        assertThat(new AntibioticId(id).value()).isEqualTo(id);
    }

    @Test
    void next_returns_distinct_ids() {
        assertThat(AntibioticId.next()).isNotEqualTo(AntibioticId.next());
    }

    @Test
    void equals_null_is_false() {
        assertThat(AntibioticId.next().equals(null)).isFalse();
    }
}
