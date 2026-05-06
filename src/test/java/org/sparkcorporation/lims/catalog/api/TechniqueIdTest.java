package org.sparkcorporation.lims.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.shared.id.Ulid;

class TechniqueIdTest {

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new TechniqueId(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_invalid_ulid() {
        assertThatThrownBy(() -> new TechniqueId("not-a-ulid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid ULID");
    }

    @Test
    void accepts_valid_ulid() {
        String id = Ulid.generate();
        assertThat(new TechniqueId(id).value()).isEqualTo(id);
    }

    @Test
    void next_returns_distinct_ids() {
        assertThat(TechniqueId.next()).isNotEqualTo(TechniqueId.next());
    }

    @Test
    void equals_null_is_false() {
        assertThat(TechniqueId.next().equals(null)).isFalse();
    }
}
