package org.sparkcorporation.lims.cashbox.infra;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.cashbox.domain.CashSession;
import org.sparkcorporation.lims.cashbox.domain.CashSessionPk;

public interface CashSessionRepository extends JpaRepository<CashSession, CashSessionPk> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CashSession> findByCashRegisterIdAndSessionDate(
            String cashRegisterId, LocalDate sessionDate);

    @Query(
            value =
                    """
                    SELECT
                      COALESCE(SUM(CASE WHEN operation_kind='PAYMENT'
                                          AND payment_mode='CASH'        THEN amount ELSE 0 END), 0) AS cash,
                      COALESCE(SUM(CASE WHEN operation_kind='PAYMENT'
                                          AND payment_mode='CHEQUE'      THEN amount ELSE 0 END), 0) AS cheque,
                      COALESCE(SUM(CASE WHEN operation_kind='PAYMENT'
                                          AND payment_mode='CARD_TPE'    THEN amount ELSE 0 END), 0) AS card_tpe,
                      COALESCE(SUM(CASE WHEN operation_kind='PAYMENT'
                                          AND payment_mode='FLOOZ'       THEN amount ELSE 0 END), 0) AS flooz,
                      COALESCE(SUM(CASE WHEN operation_kind='PAYMENT'
                                          AND payment_mode='MIXX_BY_YAS' THEN amount ELSE 0 END), 0) AS mixx_by_yas,
                      COALESCE(SUM(CASE WHEN operation_kind='EXPENSE'
                                                                         THEN amount ELSE 0 END), 0) AS expenses,
                      COALESCE(SUM(CASE WHEN operation_kind='HOME_COLLECTION_FEE'
                                                                         THEN amount ELSE 0 END), 0) AS home_collection
                    FROM   cashbox_journal_entries
                    WHERE  cash_register_id = CAST(:registerId AS varchar)
                      AND  occurred_at::date = CAST(:date AS date)
                    """,
            nativeQuery = true)
    CashSessionTotalsProjection computeTotals(
            @Param("registerId") String registerId, @Param("date") LocalDate date);
}
