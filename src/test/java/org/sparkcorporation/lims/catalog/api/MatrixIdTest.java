package org.sparkcorporation.lims.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.shared.id.Ulid;

class MatrixIdTest {

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new MatrixId(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_invalid_ulid() {
        assertThatThrownBy(() -> new MatrixId("not-a-ulid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid ULID");
    }

    @Test
    void accepts_valid_ulid() {
        String id = Ulid.generate();
        assertThat(new MatrixId(id).value()).isEqualTo(id);
    }

    @Test
    void next_returns_distinct_ids() {
        assertThat(MatrixId.next()).isNotEqualTo(MatrixId.next());
    }

    @Test
    void equals_null_is_false() {
        assertThat(MatrixId.next().equals(null)).isFalse();
    }
}
