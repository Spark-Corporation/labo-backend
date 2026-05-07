package org.sparkcorporation.lims.catalog.infra;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates server-managed business codes ({@code PRS-NNNNNN} for prescribers, {@code PHL-NNNNNN}
 * for préleveurs) by atomically reading-and-incrementing a per-entity-type counter row.
 *
 * <p>The {@link Propagation#REQUIRED} transaction on {@link #next(EntityType)} enrolls the counter
 * bump in the caller's outer transaction (typically a {@code *Service.create(...)} call), so a
 * failed entity insert also rolls back the sequence bump — guaranteeing no gaps in generated codes
 * for committed rows.
 *
 * <p>Concurrent creates serialize on the counter row via {@link
 * CatalogCodeCounterRepository#findByIdForUpdate(String)} which issues a {@code SELECT ... FOR
 * UPDATE}. Acceptable because prescriber/préleveur creation is a low-frequency admin operation —
 * see spec §4.6 for the divergence rationale vs the patient module's UPSERT pattern.
 *
 * <p>Six digits = 999 999 records per entity per lab. CDC has no upper-limit clause; widening the
 * format is out of scope for 1.3.b.i.
 */
@Service
@RequiredArgsConstructor
public class CatalogCodeGenerator {

    private final CatalogCodeCounterRepository counters;
    private final Clock clock;

    /**
     * Returns the next code for the given entity type, formatted {@code <PREFIX>-NNNNNN}. Throws
     * {@link IllegalStateException} if the counter row is missing — typically because the seed
     * changeset {@code 202605061600-seed-catalog-code-counters} did not apply (which {@link
     * CatalogCounterStartupCheck} also detects at boot).
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public String next(EntityType type) {
        var counter =
                counters.findByIdForUpdate(type.name())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "catalog_code_counters row missing for "
                                                        + type
                                                        + " — seed changeset 202605061600 did not"
                                                        + " apply"));
        long sequence = counter.incrementAndGet(clock);
        return String.format("%s-%06d", type.prefix(), sequence);
    }

    /** Counter-keyed entity type. {@link #name()} is the primary-key value in the counter table. */
    public enum EntityType {
        PRESCRIBER("PRS"),
        PRELEVEUR("PHL");

        private final String prefix;

        EntityType(String prefix) {
            this.prefix = prefix;
        }

        public String prefix() {
            return prefix;
        }
    }
}
