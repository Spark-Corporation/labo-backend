package org.sparkcorporation.lims.billing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.billing.api.InvoiceNumber;
import org.sparkcorporation.lims.billing.infra.InvoiceNumberGenerator;

/**
 * Concurrency acceptance IT for {@link InvoiceNumberGenerator} per spec §5.5 — load-bearing test
 * for the pessimistic-write-lock serialization claim. Spawns 20 parallel {@code next()} calls
 * against the same {@code (labCode, date)} bucket and asserts:
 *
 * <ol>
 *   <li>No duplicate invoice numbers across the 20 results.
 *   <li>The sequence is exactly {@code 001..020} (or whatever the starting offset implies) with no
 *       gaps.
 * </ol>
 *
 * <p>If pessimistic locking were broken, two threads racing on the same bucket would either return
 * the same sequence value (duplicate) or skip values (gap).
 */
@Tag("integration")
@SpringBootTest
class InvoiceNumberGeneratorConcurrencyIT extends IntegrationTestBase {

    private static final int THREADS = 20;

    @Autowired private InvoiceNumberGenerator generator;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void parallel_next_calls_yield_no_duplicates_and_no_gaps() throws Exception {
        // Reset the counter row for today so the load-bearing 1..20 invariant is asserted from a
        // known zero state (other ITs in the same JVM-singleton container may have bumped it).
        var today = LocalDate.now();
        jdbc.update(
                "DELETE FROM invoice_daily_counters WHERE lab_code = 'LAB' AND date = ?", today);

        var executor = Executors.newFixedThreadPool(THREADS);
        var futures = new ArrayList<Future<InvoiceNumber>>();
        for (int i = 0; i < THREADS; i++) {
            futures.add(executor.submit(() -> generator.next(today)));
        }
        executor.shutdown();
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            throw new AssertionError("executor did not terminate in 60s");
        }

        var numbers = new ArrayList<String>();
        for (var f : futures) {
            numbers.add(f.get().value());
        }

        // No duplicates.
        assertThat(numbers).doesNotHaveDuplicates();
        assertThat(numbers).hasSize(THREADS);

        // Sequential 001..020 (or 020..001 sorted ascending).
        var sorted = numbers.stream().sorted().toList();
        for (int i = 0; i < THREADS; i++) {
            assertThat(sorted.get(i)).endsWith("-%03d".formatted(i + 1));
        }
    }
}
