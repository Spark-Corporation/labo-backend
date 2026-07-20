package org.sparkcorporation.lims.iam.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.shared.id.Ulid;

class UserIdTest {

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new UserId(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_invalid_ulid() {
        assertThatThrownBy(() -> new UserId("not-a-ulid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid ULID");
    }

    @Test
    void accepts_valid_ulid() {
        String id = Ulid.generate();
        assertThat(new UserId(id).value()).isEqualTo(id);
    }

    @Test
    void next_returns_distinct_ids() {
        assertThat(UserId.next()).isNotEqualTo(UserId.next());
    }

    @Test
    void equals_null_is_false() {
        assertThat(UserId.next().equals(null)).isFalse();
    }
}
