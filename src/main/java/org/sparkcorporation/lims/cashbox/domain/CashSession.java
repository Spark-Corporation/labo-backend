package org.sparkcorporation.lims.cashbox.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.cashbox.api.SessionStatus;
import org.sparkcorporation.lims.iam.api.UserId;

@Entity
@Table(name = "cashbox_sessions")
@IdClass(CashSessionPk.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CashSession {

    @Id
    @Column(name = "cash_register_id", length = 26)
    private String cashRegisterId;

    @Id
    @Column(name = "session_date")
    private LocalDate sessionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SessionStatus status;

    @Column(name = "cash_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal cashTotal = BigDecimal.ZERO;

    @Column(name = "cheque_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal chequeTotal = BigDecimal.ZERO;

    @Column(name = "card_tpe_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal cardTpeTotal = BigDecimal.ZERO;

    @Column(name = "flooz_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal floozTotal = BigDecimal.ZERO;

    @Column(name = "mixx_by_yas_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal mixxByYasTotal = BigDecimal.ZERO;

    @Column(name = "expenses_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal expensesTotal = BigDecimal.ZERO;

    @Column(name = "home_collection_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal homeCollectionTotal = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(name = "closed_by", length = 26)
    private String closedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "reopened_by", length = 26)
    private String reopenedBy;

    @Column(name = "reopened_at")
    private Instant reopenedAt;

    @Column(name = "reopen_reason")
    private String reopenReason;

    @Version private long version;

    public static CashSession openIfMissing(CashRegisterId registerId, LocalDate date) {
        var s = new CashSession();
        s.cashRegisterId = registerId.value();
        s.sessionDate = date;
        s.status = SessionStatus.OPEN;
        return s;
    }

    public record Totals(
            BigDecimal cash,
            BigDecimal cheque,
            BigDecimal cardTpe,
            BigDecimal flooz,
            BigDecimal mixxByYas,
            BigDecimal expenses,
            BigDecimal homeCollection) {}

    public void close(Totals totals, UserId closedBy, Clock clock) {
        if (status != SessionStatus.OPEN) {
            throw new CashSessionNotOpenException(cashRegisterId, sessionDate);
        }
        this.cashTotal = totals.cash();
        this.chequeTotal = totals.cheque();
        this.cardTpeTotal = totals.cardTpe();
        this.floozTotal = totals.flooz();
        this.mixxByYasTotal = totals.mixxByYas();
        this.expensesTotal = totals.expenses();
        this.homeCollectionTotal = totals.homeCollection();
        var payments =
                totals.cash()
                        .add(totals.cheque())
                        .add(totals.cardTpe())
                        .add(totals.flooz())
                        .add(totals.mixxByYas());
        this.grandTotal = payments.subtract(totals.expenses()).add(totals.homeCollection());
        this.status = SessionStatus.CLOSED;
        this.closedBy = closedBy.value();
        this.closedAt = Instant.now(clock);
    }

    public void reopen(String reason, UserId reopenedBy, Clock clock) {
        if (status != SessionStatus.CLOSED) {
            throw new CashSessionNotClosedException(cashRegisterId, sessionDate);
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reopen reason must not be blank");
        }
        this.status = SessionStatus.OPEN;
        this.reopenedBy = reopenedBy.value();
        this.reopenedAt = Instant.now(clock);
        this.reopenReason = reason.strip();
    }
}
