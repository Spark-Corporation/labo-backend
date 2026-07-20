package org.sparkcorporation.lims.billing.infra;

import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.billing.api.InvoiceNumber;

/**
 * Generates strictly monotonic, gap-free invoice numbers per {@code (labCode, date)} bucket. Format
 * per CDC Gestion comptable §1: {@code <labCode><yy>-<ddMM>-<seq>} where {@code seq} is zero-padded
 * to at least 3 digits. Example from CDC: a Togo invoice on 17/10/2024 with seq 5 → {@code
 * LTA24-1710-005}.
 *
 * <p>Concurrency: a single Postgres {@code INSERT … ON CONFLICT DO UPDATE … RETURNING} performs the
 * read-modify-write atomically — the row-level lock created by {@code ON CONFLICT DO UPDATE}
 * serializes concurrent calls without needing {@code SERIALIZABLE} isolation. Mirrors the {@code
 * JpaLabFileRegistrationCounter} pattern from 1.4. The surrounding {@code @Transactional}
 * (REQUIRED) enrolls the call in the caller's transaction so an invoice-issuance rollback also
 * rolls back the counter bump.
 */
@Service
@RequiredArgsConstructor
public class InvoiceNumberGenerator {

    private static final String UPSERT_SQL =
            """
            INSERT INTO invoice_daily_counters (lab_code, date, last_seq)
            VALUES (?, ?, 1)
            ON CONFLICT (lab_code, date) DO UPDATE
              SET last_seq = invoice_daily_counters.last_seq + 1
            RETURNING last_seq
            """;

    private final EntityManager em;
    private final LabProperties labProperties;

    @Transactional(propagation = Propagation.REQUIRED)
    public InvoiceNumber next(LocalDate date) {
        Object raw =
                em.createNativeQuery(UPSERT_SQL)
                        .setParameter(1, labProperties.code())
                        .setParameter(2, date)
                        .getSingleResult();
        int seq = ((Number) raw).intValue();
        return new InvoiceNumber(format(labProperties.code(), date, seq));
    }

    private static final int YEAR_MOD = 100;

    static String format(String labCode, LocalDate date, int seq) {
        return "%s%02d-%02d%02d-%03d"
                .formatted(
                        labCode,
                        date.getYear() % YEAR_MOD,
                        date.getDayOfMonth(),
                        date.getMonthValue(),
                        seq);
    }
}
