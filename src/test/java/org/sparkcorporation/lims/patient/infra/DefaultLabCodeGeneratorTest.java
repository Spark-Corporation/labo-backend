package org.sparkcorporation.lims.patient.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.patient.domain.LabCodeCounter;
import org.sparkcorporation.lims.patient.domain.LabCodePrefix;

class DefaultLabCodeGeneratorTest {

    /** Pure in-memory counter fake; per (prefix, day) increments. */
    static class InMemoryCounter implements LabCodeCounter {
        private final Map<Entry<LabCodePrefix, LocalDate>, Integer> store = new HashMap<>();

        @Override
        public int incrementAndGet(LabCodePrefix prefix, LocalDate day) {
            return store.merge(Map.entry(prefix, day), 1, Integer::sum);
        }
    }

    private final InMemoryCounter counter = new InMemoryCounter();
    private final DefaultLabCodeGenerator generator =
            new DefaultLabCodeGenerator(counter, new LabCodePrefix("LAB"));

    @Test
    void formats_first_code_with_3_digit_seq() {
        var code = generator.next(LocalDate.of(2026, 5, 5));
        assertThat(code.value()).isEqualTo("LAB-260505-001");
    }

    @Test
    void increments_within_same_day() {
        var day = LocalDate.of(2026, 5, 5);
        assertThat(generator.next(day).value()).isEqualTo("LAB-260505-001");
        assertThat(generator.next(day).value()).isEqualTo("LAB-260505-002");
        assertThat(generator.next(day).value()).isEqualTo("LAB-260505-003");
    }

    @Test
    void resets_per_day() {
        assertThat(generator.next(LocalDate.of(2026, 5, 5)).value()).isEqualTo("LAB-260505-001");
        assertThat(generator.next(LocalDate.of(2026, 5, 6)).value()).isEqualTo("LAB-260506-001");
    }

    @Test
    void uses_4_digit_seq_for_busy_days() {
        var day = LocalDate.of(2026, 5, 5);
        for (int i = 0; i < 999; i++) generator.next(day);
        var code = generator.next(day);
        assertThat(code.value()).isEqualTo("LAB-260505-1000");
    }

    @Test
    void produces_distinct_codes_for_50_consecutive_calls() {
        var day = LocalDate.of(2026, 5, 5);
        var produced = new java.util.HashSet<String>();
        for (int i = 0; i < 50; i++) {
            produced.add(generator.next(day).value());
        }
        assertThat(produced).hasSize(50);
    }
}
