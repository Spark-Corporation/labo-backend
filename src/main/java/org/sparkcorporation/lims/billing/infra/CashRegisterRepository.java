package org.sparkcorporation.lims.billing.infra;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.billing.domain.CashRegister;

/**
 * Spring Data JPA repository for {@link CashRegister}. Note: {@code CashRegister} carries
 * {@code @SQLRestriction("disabled_at IS NULL")}, so standard finders (e.g. {@link #findById})
 * return empty for disabled rows. The {@code searchNative} query bypasses Hibernate's restriction
 * (native SQL) so callers can list disabled rows when {@code disabled=true} is requested.
 */
public interface CashRegisterRepository extends JpaRepository<CashRegister, String> {

    boolean existsByCode(String code);

    /**
     * Native search bypassing {@code @SQLRestriction}. {@code q} matches against {@code code} OR
     * {@code label} (case-insensitive LIKE). {@code disabled = null} → both active and disabled
     * rows; {@code true} → only disabled; {@code false} → only active.
     */
    @Query(
            value =
                    """
                    SELECT * FROM cash_registers
                    WHERE (:q IS NULL
                           OR LOWER(code) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
                           OR LOWER(label) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))
                      AND (:disabled IS NULL
                           OR (:disabled = TRUE  AND disabled_at IS NOT NULL)
                           OR (:disabled = FALSE AND disabled_at IS NULL))
                    ORDER BY sequence_number
                    """,
            countQuery =
                    """
                    SELECT count(*) FROM cash_registers
                    WHERE (:q IS NULL
                           OR LOWER(code) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
                           OR LOWER(label) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))
                      AND (:disabled IS NULL
                           OR (:disabled = TRUE  AND disabled_at IS NOT NULL)
                           OR (:disabled = FALSE AND disabled_at IS NULL))
                    """,
            nativeQuery = true)
    Page<CashRegister> searchNative(
            @Param("q") @Nullable String q,
            @Param("disabled") @Nullable Boolean disabled,
            Pageable pageable);

    /**
     * Exposed for chunk 7's {@code PaymentService}: returns true if {@code userId} is currently
     * assigned to {@code registerId}. Implemented as a JPQL count to skip a full {@code
     * CashRegister} fetch.
     */
    @Query(
            """
            SELECT COUNT(a) > 0 FROM CashRegisterAssignment a
            WHERE a.id.cashRegisterId = :registerId AND a.id.userId = :userId
            """)
    boolean isUserAssigned(@Param("registerId") String registerId, @Param("userId") String userId);
}
