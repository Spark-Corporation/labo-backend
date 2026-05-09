package org.sparkcorporation.lims.order.api;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class QuoteNumberTest {

    @Test
    void regex_accepts_DEV_yyMMdd_NNNN_format() {
        assertThatCode(() -> new QuoteNumber("DEV-260508-0001")).doesNotThrowAnyException();
        assertThatCode(() -> new QuoteNumber("DEV-260508-9999")).doesNotThrowAnyException();
    }

    @Test
    void rejects_missing_DEV_prefix() {
        assertThatThrownBy(() -> new QuoteNumber("LF-260508-0001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid quote number");
    }

    @Test
    void rejects_wrong_date_digit_count() {
        assertThatThrownBy(() -> new QuoteNumber("DEV-26058-0001"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_wrong_sequence_digit_count() {
        assertThatThrownBy(() -> new QuoteNumber("DEV-260508-001"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new QuoteNumber("DEV-260508-12345"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new QuoteNumber(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
