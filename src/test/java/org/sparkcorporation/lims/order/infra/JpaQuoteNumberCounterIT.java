package org.sparkcorporation.lims.order.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import org.sparkcorporation.lims.IntegrationTestBase;

/**
 * Round-trip test for the UPSERT-with-RETURNING per-day quote-number counter. Docker-blocked
 * locally (Testcontainers PostgreSQL via {@link IntegrationTestBase}); runs in CI. Mirrors {@link
 * JpaLabFileRegistrationCounterIT}.
 */
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaQuoteNumberCounter.class)
class JpaQuoteNumberCounterIT extends IntegrationTestBase {

    @Autowired private JpaQuoteNumberCounter counter;

    @Test
    void upsert_round_trip_increments_atomically() {
        var day = LocalDate.of(2026, 5, 8);
        assertThat(counter.incrementAndGet(day)).isEqualTo(1);
        assertThat(counter.incrementAndGet(day)).isEqualTo(2);
        assertThat(counter.incrementAndGet(day)).isEqualTo(3);

        // different day gets its own counter
        assertThat(counter.incrementAndGet(day.plusDays(1))).isEqualTo(1);
        // back to first day continues
        assertThat(counter.incrementAndGet(day)).isEqualTo(4);
    }
}
