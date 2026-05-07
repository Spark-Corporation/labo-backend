package org.sparkcorporation.lims.catalog.infra;

import java.time.Clock;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Per-entity-type counter row backing the {@code PRS-NNNNNN} / {@code PHL-NNNNNN} auto-generated
 * business codes (spec §4.6).
 *
 * <p>The counter row is locked pessimistically by {@link
 * CatalogCodeCounterRepository#findByIdForUpdate(String)} from inside {@link
 * CatalogCodeGenerator#next(CatalogCodeGenerator.EntityType)}, which then calls {@link
 * #incrementAndGet(Clock)} to read the current sequence and bump it for the next caller. The
 * surrounding {@code @Transactional} on the generator enrolls the counter update in the caller's
 * transaction so a failed entity insert also rolls back the sequence bump.
 *
 * <p>The {@link Version} column is project convention (every catalog table carries one); the
 * pessimistic lock — not optimistic concurrency — is what serializes concurrent calls.
 */
@Entity
@Table(name = "catalog_code_counters")
public class CatalogCodeCounter {

    @Id
    @Column(name = "entity_type", length = 20, nullable = false)
    private String entityType;

    @Column(name = "next_sequence", nullable = false)
    private long nextSequence;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** JPA constructor. */
    protected CatalogCodeCounter() {}

    /** Package-private all-args constructor for unit tests; production loads via JPA. */
    CatalogCodeCounter(String entityType, long nextSequence, long version, Instant updatedAt) {
        this.entityType = entityType;
        this.nextSequence = nextSequence;
        this.version = version;
        this.updatedAt = updatedAt;
    }

    /**
     * Returns the current {@code nextSequence} and increments it by one, stamping {@code
     * updatedAt}. Caller MUST hold the pessimistic write lock acquired via {@link
     * CatalogCodeCounterRepository#findByIdForUpdate(String)}.
     */
    public long incrementAndGet(Clock clock) {
        long current = nextSequence;
        nextSequence = current + 1;
        updatedAt = Instant.now(clock);
        return current;
    }

    public String entityType() {
        return entityType;
    }

    public long nextSequence() {
        return nextSequence;
    }

    public long version() {
        return version;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
