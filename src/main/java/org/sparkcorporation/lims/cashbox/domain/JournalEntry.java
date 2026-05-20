package org.sparkcorporation.lims.cashbox.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.billing.api.PaymentMode;
import org.sparkcorporation.lims.cashbox.api.JournalEntryId;
import org.sparkcorporation.lims.cashbox.api.OperationKind;
import org.sparkcorporation.lims.iam.api.UserId;

@Entity
@Table(name = "cashbox_journal_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JournalEntry {

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_kind", nullable = false, length = 32)
    private OperationKind operationKind;

    @Column(name = "source_id", nullable = false, length = 32)
    private String sourceId;

    @Column(name = "cash_register_id", nullable = false, length = 26)
    private String cashRegisterId;

    @Column(name = "user_id", nullable = false, length = 26)
    private String userId;

    @Column(name = "lab_file_id", length = 26)
    private String labFileId;

    @Column(name = "patient_id", length = 26)
    private String patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", length = 16)
    private PaymentMode paymentMode;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public static JournalEntry forExpense(
            String expenseId,
            CashRegisterId registerId,
            UserId userId,
            PaymentMode mode,
            BigDecimal amount,
            Instant occurredAt) {
        validateAmount(amount);
        Objects.requireNonNull(mode, "mode required for EXPENSE");
        var e = new JournalEntry();
        e.id = JournalEntryId.generate().value();
        e.operationKind = OperationKind.EXPENSE;
        e.sourceId = expenseId;
        e.cashRegisterId = registerId.value();
        e.userId = userId.value();
        e.paymentMode = mode;
        e.amount = amount;
        e.occurredAt = occurredAt;
        return e;
    }

    public static JournalEntry forHomeCollection(
            String feeId,
            CashRegisterId registerId,
            UserId userId,
            String labFileId,
            String patientId,
            BigDecimal amount,
            Instant occurredAt) {
        validateAmount(amount);
        var e = new JournalEntry();
        e.id = JournalEntryId.generate().value();
        e.operationKind = OperationKind.HOME_COLLECTION_FEE;
        e.sourceId = feeId;
        e.cashRegisterId = registerId.value();
        e.userId = userId.value();
        e.labFileId = labFileId;
        e.patientId = patientId;
        e.paymentMode = null;
        e.amount = amount;
        e.occurredAt = occurredAt;
        return e;
    }

    public static JournalEntry forPayment(
            String paymentId,
            CashRegisterId registerId,
            UserId userId,
            String labFileId,
            PaymentMode mode,
            BigDecimal amount,
            Instant occurredAt) {
        validateAmount(amount);
        Objects.requireNonNull(mode, "mode required for PAYMENT");
        var e = new JournalEntry();
        e.id = JournalEntryId.generate().value();
        e.operationKind = OperationKind.PAYMENT;
        e.sourceId = paymentId;
        e.cashRegisterId = registerId.value();
        e.userId = userId.value();
        e.labFileId = labFileId;
        e.paymentMode = mode;
        e.amount = amount;
        e.occurredAt = occurredAt;
        return e;
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
    }

    public JournalEntryId journalEntryId() {
        return new JournalEntryId(id);
    }
}
