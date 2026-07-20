package org.sparkcorporation.lims.patient.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.patient.domain.LabCodePrefix;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaLabCodeCounter.class)
class JpaLabCodeCounterTest extends IntegrationTestBase {

    @Autowired private JpaLabCodeCounter counter;

    @Test
    void first_call_returns_1() {
        int seq = counter.incrementAndGet(new LabCodePrefix("LAB"), LocalDate.of(2026, 5, 5));
        assertThat(seq).isEqualTo(1);
    }

    @Test
    void successive_calls_same_day_increment() {
        var prefix = new LabCodePrefix("LAB");
        var day = LocalDate.of(2026, 5, 5);
        assertThat(counter.incrementAndGet(prefix, day)).isEqualTo(1);
        assertThat(counter.incrementAndGet(prefix, day)).isEqualTo(2);
        assertThat(counter.incrementAndGet(prefix, day)).isEqualTo(3);
    }

    @Test
    void per_day_reset() {
        var prefix = new LabCodePrefix("LAB");
        assertThat(counter.incrementAndGet(prefix, LocalDate.of(2026, 5, 5))).isEqualTo(1);
        assertThat(counter.incrementAndGet(prefix, LocalDate.of(2026, 5, 6))).isEqualTo(1);
        assertThat(counter.incrementAndGet(prefix, LocalDate.of(2026, 5, 5))).isEqualTo(2);
    }

    @Test
    void different_prefixes_independent() {
        var day = LocalDate.of(2026, 5, 5);
        assertThat(counter.incrementAndGet(new LabCodePrefix("LAB"), day)).isEqualTo(1);
        assertThat(counter.incrementAndGet(new LabCodePrefix("LBA"), day)).isEqualTo(1);
        assertThat(counter.incrementAndGet(new LabCodePrefix("LAB"), day)).isEqualTo(2);
    }

    @Test
    void concurrent_increments_produce_distinct_values() throws Exception {
        var prefix = new LabCodePrefix("LAB");
        var day = LocalDate.of(2026, 5, 5);
        int n = 50;
        ExecutorService pool = Executors.newFixedThreadPool(10);
        try {
            List<CompletableFuture<Integer>> futures =
                    IntStream.range(0, n)
                            .mapToObj(
                                    i ->
                                            CompletableFuture.supplyAsync(
                                                    () -> counter.incrementAndGet(prefix, day),
                                                    pool))
                            .toList();
            Set<Integer> distinct = new HashSet<>();
            for (var f : futures) distinct.add(f.get());
            assertThat(distinct).hasSize(n);
        } finally {
            pool.shutdown();
        }
    }
}
