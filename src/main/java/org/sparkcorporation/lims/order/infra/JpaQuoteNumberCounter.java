package org.sparkcorporation.lims.order.infra;

import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.order.domain.QuoteNumberCounter;

/**
 * PostgreSQL UPSERT-with-RETURNING per-day counter for {@code DEV-yyMMdd-NNNN} numbers. Mirrors
 * {@link JpaLabFileRegistrationCounter}; row-lock-under-contention behavior is documented in {@code
 * 2026-05-07-order-design.md} §4.5. The surrounding {@code @Transactional} (REQUIRED) enrolls the
 * call in the caller's transaction so a quote-creation rollback also rolls back the counter bump.
 */
@Component
@RequiredArgsConstructor
public class JpaQuoteNumberCounter implements QuoteNumberCounter {

    private static final String UPSERT_SQL =
            """
            INSERT INTO quote_number_counters (day, sequence)
            VALUES (?, 1)
            ON CONFLICT (day) DO UPDATE
              SET sequence = quote_number_counters.sequence + 1
            RETURNING sequence
            """;

    private final EntityManager em;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public int incrementAndGet(LocalDate day) {
        Object result = em.createNativeQuery(UPSERT_SQL).setParameter(1, day).getSingleResult();
        return ((Number) result).intValue();
    }
}
