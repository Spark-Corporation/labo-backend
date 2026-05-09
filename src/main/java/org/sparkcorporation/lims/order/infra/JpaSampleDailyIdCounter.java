package org.sparkcorporation.lims.order.infra;

import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.order.domain.SampleDailyIdCounter;

/**
 * PostgreSQL-backed {@link SampleDailyIdCounter} using upsert with {@code RETURNING} for atomic
 * read-modify-write in a single SQL statement (spec §4.5). Two-key composite primary key is {@code
 * (day, discipline_prefix)}; the {@code disciplinePrefix} {@code int} (1..5) is bound as a {@code
 * CHAR(1)} via {@link String#valueOf(int)} per the plan. The surrounding {@code @Transactional}
 * (REQUIRED) enrolls the call in the caller's transaction so a sample-create rollback also rolls
 * back the counter bump.
 */
@Component
@RequiredArgsConstructor
public class JpaSampleDailyIdCounter implements SampleDailyIdCounter {

    private static final String UPSERT_SQL =
            """
            INSERT INTO sample_daily_id_counters (day, discipline_prefix, sequence)
            VALUES (?, ?, 1)
            ON CONFLICT (day, discipline_prefix) DO UPDATE
              SET sequence = sample_daily_id_counters.sequence + 1
            RETURNING sequence
            """;

    private final EntityManager em;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public int incrementAndGet(LocalDate day, int disciplinePrefix) {
        Object result =
                em.createNativeQuery(UPSERT_SQL)
                        .setParameter(1, day)
                        .setParameter(2, String.valueOf(disciplinePrefix))
                        .getSingleResult();
        return ((Number) result).intValue();
    }
}
