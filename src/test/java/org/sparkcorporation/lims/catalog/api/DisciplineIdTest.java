package org.sparkcorporation.lims.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.shared.id.Ulid;

class DisciplineIdTest {

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new DisciplineId(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_invalid_ulid() {
        assertThatThrownBy(() -> new DisciplineId("not-a-ulid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid ULID");
    }

    @Test
    void accepts_valid_ulid() {
        String id = Ulid.generate();
        assertThat(new DisciplineId(id).value()).isEqualTo(id);
    }

    @Test
    void next_returns_distinct_ids() {
        assertThat(DisciplineId.next()).isNotEqualTo(DisciplineId.next());
    }

    @Test
    void equals_null_is_false() {
        assertThat(DisciplineId.next().equals(null)).isFalse();
    }
}
