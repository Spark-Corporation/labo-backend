package org.sparkcorporation.lims.order.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.order.domain.SampleDailyIdCounter;

class DefaultSampleDailyIdGeneratorTest {

    private final SampleDailyIdCounter counter = mock(SampleDailyIdCounter.class);
    private final DefaultSampleDailyIdGenerator gen = new DefaultSampleDailyIdGenerator(counter);

    @Test
    void format_zero_pads_to_three_digits_and_uses_french_ddmmyy_order() {
        when(counter.incrementAndGet(LocalDate.of(2026, 5, 7), 1)).thenReturn(1);
        assertThat(gen.next(1, LocalDate.of(2026, 5, 7)).value()).isEqualTo("1070526-001");
    }

    @Test
    void format_zero_pads_high_sequence_to_three_digits() {
        when(counter.incrementAndGet(LocalDate.of(2026, 5, 7), 2)).thenReturn(42);
        assertThat(gen.next(2, LocalDate.of(2026, 5, 7)).value()).isEqualTo("2070526-042");
    }

    @Test
    void format_handles_max_three_digit_sequence() {
        when(counter.incrementAndGet(LocalDate.of(2026, 12, 31), 5)).thenReturn(999);
        assertThat(gen.next(5, LocalDate.of(2026, 12, 31)).value()).isEqualTo("5311226-999");
    }

    @Test
    void rejects_prefix_below_one() {
        assertThatThrownBy(() -> gen.next(0, LocalDate.of(2026, 5, 7)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disciplinePrefix");
    }

    @Test
    void rejects_prefix_above_five() {
        assertThatThrownBy(() -> gen.next(6, LocalDate.of(2026, 5, 7)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disciplinePrefix");
    }
}
