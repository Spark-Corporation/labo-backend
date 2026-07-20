package org.sparkcorporation.lims.order.infra;

import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.order.domain.LabFileRegistrationCounter;

/**
 * PostgreSQL-backed {@link LabFileRegistrationCounter} using upsert with {@code RETURNING} for
 * atomic read-modify-write in a single SQL statement (spec §4.5). The row-level lock created by
 * {@code INSERT ... ON CONFLICT DO UPDATE} serializes concurrent calls without needing {@code
 * SERIALIZABLE} isolation; the surrounding {@code @Transactional} (REQUIRED) enrolls the call in
 * the caller's transaction so a lab-file-creation rollback also rolls back the counter bump.
 */
@Component
@RequiredArgsConstructor
public class JpaLabFileRegistrationCounter implements LabFileRegistrationCounter {

    private static final String UPSERT_SQL =
            """
            INSERT INTO lab_file_registration_counters (day, sequence)
            VALUES (?, 1)
            ON CONFLICT (day) DO UPDATE
              SET sequence = lab_file_registration_counters.sequence + 1
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
