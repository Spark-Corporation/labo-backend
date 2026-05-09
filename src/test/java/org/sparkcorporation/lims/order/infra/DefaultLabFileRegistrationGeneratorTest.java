package org.sparkcorporation.lims.order.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.order.domain.LabFileRegistrationCounter;

class DefaultLabFileRegistrationGeneratorTest {

    private final LabFileRegistrationCounter counter = mock(LabFileRegistrationCounter.class);
    private final DefaultLabFileRegistrationGenerator gen =
            new DefaultLabFileRegistrationGenerator(counter);

    @Test
    void format_zero_pads_sequence_to_four_digits_and_uses_yymmdd_order() {
        when(counter.incrementAndGet(LocalDate.of(2026, 5, 7))).thenReturn(1);
        assertThat(gen.next(LocalDate.of(2026, 5, 7)).value()).isEqualTo("LF-260507-0001");
    }

    @Test
    void format_handles_high_four_digit_sequence() {
        when(counter.incrementAndGet(LocalDate.of(2026, 5, 7))).thenReturn(9999);
        assertThat(gen.next(LocalDate.of(2026, 5, 7)).value()).isEqualTo("LF-260507-9999");
    }

    @Test
    void format_handles_year_end_date() {
        when(counter.incrementAndGet(LocalDate.of(2026, 12, 31))).thenReturn(42);
        assertThat(gen.next(LocalDate.of(2026, 12, 31)).value()).isEqualTo("LF-261231-0042");
    }
}
