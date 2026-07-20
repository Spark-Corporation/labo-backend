package org.sparkcorporation.lims.billing.infra;

import org.springframework.data.jpa.repository.JpaRepository;

import org.sparkcorporation.lims.billing.domain.CounterKey;
import org.sparkcorporation.lims.billing.domain.InvoiceDailyCounter;

/**
 * Spring Data JPA repository for {@link InvoiceDailyCounter}. The aggregate's read-modify-write
 * loop runs through {@link InvoiceNumberGenerator}'s native {@code INSERT … ON CONFLICT DO UPDATE
 * RETURNING} pattern (spec §5.5); this repo exists for the rare diagnostic / admin lookup.
 */
public interface InvoiceDailyCounterRepository
        extends JpaRepository<InvoiceDailyCounter, CounterKey> {}
