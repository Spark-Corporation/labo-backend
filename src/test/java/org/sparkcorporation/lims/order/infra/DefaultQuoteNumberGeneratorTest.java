package org.sparkcorporation.lims.order.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.order.domain.QuoteNumberCounter;

class DefaultQuoteNumberGeneratorTest {

    private final QuoteNumberCounter counter = mock(QuoteNumberCounter.class);
    private final DefaultQuoteNumberGenerator gen = new DefaultQuoteNumberGenerator(counter);

    @Test
    void format_zero_pads_to_four_digits_and_uses_yymmdd_order() {
        when(counter.incrementAndGet(LocalDate.of(2026, 5, 8))).thenReturn(1);
        assertThat(gen.next(LocalDate.of(2026, 5, 8)).value()).isEqualTo("DEV-260508-0001");
    }

    @Test
    void format_handles_high_four_digit_sequence() {
        when(counter.incrementAndGet(LocalDate.of(2026, 5, 8))).thenReturn(9999);
        assertThat(gen.next(LocalDate.of(2026, 5, 8)).value()).isEqualTo("DEV-260508-9999");
    }

    @Test
    void format_uses_correct_year_and_month_padding() {
        when(counter.incrementAndGet(LocalDate.of(2026, 1, 1))).thenReturn(42);
        assertThat(gen.next(LocalDate.of(2026, 1, 1)).value()).isEqualTo("DEV-260101-0042");
    }
}
