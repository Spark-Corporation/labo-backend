package org.sparkcorporation.lims.patient.infra;

import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.patient.domain.LabCodeCounter;
import org.sparkcorporation.lims.patient.domain.LabCodePrefix;

/**
 * PostgreSQL-backed {@link LabCodeCounter} using upsert with {@code RETURNING} for atomic
 * read-modify-write in a single SQL statement. The row-level lock created by {@code INSERT ... ON
 * CONFLICT DO UPDATE} serializes concurrent calls without needing {@code SERIALIZABLE} isolation;
 * the surrounding {@code @Transactional} (REQUIRED, default) enrolls the call in the caller's
 * transaction so a patient-creation rollback also rolls back the counter bump.
 */
@Component
@RequiredArgsConstructor
public class JpaLabCodeCounter implements LabCodeCounter {

    private static final String UPSERT_SQL =
            """
            INSERT INTO patient_code_counters (lab_prefix, day, sequence)
            VALUES (?, ?, 1)
            ON CONFLICT (lab_prefix, day) DO UPDATE
            SET sequence = patient_code_counters.sequence + 1
            RETURNING sequence
            """;

    private final EntityManager em;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public int incrementAndGet(LabCodePrefix prefix, LocalDate day) {
        Object result =
                em.createNativeQuery(UPSERT_SQL)
                        .setParameter(1, prefix.value())
                        .setParameter(2, day)
                        .getSingleResult();
        return ((Number) result).intValue();
    }
}
