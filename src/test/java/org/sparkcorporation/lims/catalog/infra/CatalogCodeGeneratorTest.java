package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.sparkcorporation.lims.catalog.infra.CatalogCodeGenerator.EntityType;

/**
 * Unit tests for {@link CatalogCodeGenerator}: format, sequencing, missing-counter behavior. Uses a
 * fixed {@link Clock} and a mocked repository so the test stays deterministic and JPA-free.
 */
@ExtendWith(MockitoExtension.class)
class CatalogCodeGeneratorTest {

    @Mock CatalogCodeCounterRepository counters;

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void format_zero_pads_to_six_digits_for_PRESCRIBER() {
        var counter = new CatalogCodeCounter("PRESCRIBER", 1L, 0L, Instant.now(clock));
        when(counters.findByIdForUpdate("PRESCRIBER")).thenReturn(Optional.of(counter));

        var generator = new CatalogCodeGenerator(counters, clock);

        assertThat(generator.next(EntityType.PRESCRIBER)).isEqualTo("PRS-000001");
    }

    @Test
    void produces_sequential_codes_on_repeat_calls() {
        var counter = new CatalogCodeCounter("PRELEVEUR", 1L, 0L, Instant.now(clock));
        when(counters.findByIdForUpdate("PRELEVEUR")).thenReturn(Optional.of(counter));

        var generator = new CatalogCodeGenerator(counters, clock);

        assertThat(generator.next(EntityType.PRELEVEUR)).isEqualTo("PHL-000001");
        assertThat(generator.next(EntityType.PRELEVEUR)).isEqualTo("PHL-000002");
        assertThat(generator.next(EntityType.PRELEVEUR)).isEqualTo("PHL-000003");
    }

    @Test
    void six_digit_max_is_PRS_999999() {
        var counter = new CatalogCodeCounter("PRESCRIBER", 999_999L, 0L, Instant.now(clock));
        when(counters.findByIdForUpdate("PRESCRIBER")).thenReturn(Optional.of(counter));

        var generator = new CatalogCodeGenerator(counters, clock);

        assertThat(generator.next(EntityType.PRESCRIBER)).isEqualTo("PRS-999999");
    }

    @Test
    void missing_counter_row_throws() {
        when(counters.findByIdForUpdate("PRESCRIBER")).thenReturn(Optional.empty());

        var generator = new CatalogCodeGenerator(counters, clock);

        assertThatThrownBy(() -> generator.next(EntityType.PRESCRIBER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PRESCRIBER");
    }
}
