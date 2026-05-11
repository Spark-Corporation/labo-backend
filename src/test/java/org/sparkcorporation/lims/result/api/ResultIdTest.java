package org.sparkcorporation.lims.result.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ResultIdTest {

    private static final String VALID_ULID = "01KQZ0WPVQA364NMRAJMKRQC84";

    /** 26 chars but contains {@code I}, a forbidden Crockford Base32 character. */
    private static final String INVALID_CHAR_ULID = "01KQZ0WPVQA364NMRAJMKRQC8I";

    @Test
    void result_id_accepts_valid_ulid() {
        assertThat(new ResultId(VALID_ULID).value()).isEqualTo(VALID_ULID);
    }

    @Test
    void result_id_rejects_short() {
        assertThatThrownBy(() -> new ResultId("SHORT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid ULID");
    }

    @Test
    void result_id_rejects_null() {
        assertThatThrownBy(() -> new ResultId(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void result_id_rejects_forbidden_crockford_character() {
        assertThatThrownBy(() -> new ResultId(INVALID_CHAR_ULID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid ULID");
    }

    @Test
    void result_id_next_round_trips_through_constructor() {
        ResultId id = ResultId.next();
        assertThat(id.value()).hasSize(26);
        assertThat(new ResultId(id.value())).isEqualTo(id);
    }

    @Test
    void result_line_id_accepts_valid_ulid() {
        assertThat(new ResultLineId(VALID_ULID).value()).isEqualTo(VALID_ULID);
    }

    @Test
    void antibiogram_reading_id_accepts_valid_ulid() {
        assertThat(new AntibiogramReadingId(VALID_ULID).value()).isEqualTo(VALID_ULID);
    }
}
