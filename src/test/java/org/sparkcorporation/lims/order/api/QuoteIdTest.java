package org.sparkcorporation.lims.order.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.shared.id.Ulid;

class QuoteIdTest {

    @Test
    void accepts_valid_ulid() {
        var id = new QuoteId(Ulid.generate());
        assertThat(id.value()).hasSize(26);
    }

    @Test
    void rejects_invalid_ulid() {
        assertThatThrownBy(() -> new QuoteId("not-a-ulid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid ULID");
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new QuoteId(null)).isInstanceOf(NullPointerException.class);
    }
}
