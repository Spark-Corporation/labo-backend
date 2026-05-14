package org.sparkcorporation.lims.billing.domain;

import java.math.BigDecimal;
import java.time.Clock;
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
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.api.InvoiceId;
import org.sparkcorporation.lims.billing.api.PaymentId;
import org.sparkcorporation.lims.billing.api.PaymentMode;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Payment aggregate root — N:1 with {@link org.sparkcorporation.lims.order.domain.LabFile},
 * optional link to {@link Invoice} via {@code invoiceId} (populated either at insert time if the
 * invoice already exists, or back-filled by {@code BillingFinalizationService} once bio-validation
 * finalizes the invoice).
 *
 * <p>Per spec §5.4 invariants: amount must be strictly positive (matches DB CHECK {@code
 * payments_amount_positive_chk}); aggregate is immutable post-creation except for the {@link
 * #linkToInvoice(InvoiceId)} mutator which is one-shot.
 *
 * <p>Construction goes through {@link #record} only; the protected no-args constructor is reserved
 * for Hibernate.
 */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @Column(name = "lab_file_id", nullable = false, length = 26, updatable = false)
    private String labFileId;

    @Column(name = "invoice_id", length = 26)
    @Nullable
    private String invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20, updatable = false)
    private PaymentMode mode;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "cash_register_id", nullable = false, length = 26, updatable = false)
    private String cashRegisterId;

    @Column(name = "recorded_by_user_id", nullable = false, length = 26, updatable = false)
    private String recordedByUserId;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Column(name = "note", length = 500, updatable = false)
    @Nullable
    private String note;

    /**
     * Build a new {@link Payment} aggregate. Validates the positive-amount invariant; copies all
     * other fields verbatim from {@code req}. The invoice link is left null — the caller (typically
     * {@code PaymentService}) decides whether to populate it immediately via {@link
     * #linkToInvoice(InvoiceId)}.
     */
    public static Payment record(NewPayment req, Clock clock) {
        Objects.requireNonNull(req, "req");
        Objects.requireNonNull(clock, "clock");
        Objects.requireNonNull(req.labFileId(), "labFileId");
        Objects.requireNonNull(req.mode(), "mode");
        Objects.requireNonNull(req.amount(), "amount");
        Objects.requireNonNull(req.cashRegisterId(), "cashRegisterId");
        Objects.requireNonNull(req.recordedByUserId(), "recordedByUserId");
        if (req.amount().signum() <= 0) {
            throw new IllegalArgumentException(
                    "payment amount must be > 0; got " + req.amount().toPlainString());
        }
        var p = new Payment();
        p.id = Ulid.generate();
        p.labFileId = req.labFileId();
        p.mode = req.mode();
        p.amount = req.amount();
        p.cashRegisterId = req.cashRegisterId();
        p.recordedByUserId = req.recordedByUserId();
        p.recordedAt = Instant.now(clock);
        p.note = req.note();
        return p;
    }

    /**
     * Link this payment to its invoice. One-shot: re-linking throws {@link IllegalStateException}.
     * Called either by {@code PaymentService.record} (when an invoice already exists for the
     * lab-file at insert time) or by {@code BillingFinalizationService} on the bulk back-fill path
     * (when the invoice is finalized after the payments were taken).
     */
    public void linkToInvoice(InvoiceId invoiceId) {
        Objects.requireNonNull(invoiceId, "invoiceId");
        if (this.invoiceId != null) {
            throw new IllegalStateException(
                    "payment " + id + " already linked to invoice " + this.invoiceId);
        }
        this.invoiceId = invoiceId.value();
    }

    public PaymentId paymentId() {
        return PaymentId.of(id);
    }

    @Nullable
    public InvoiceId invoiceIdTyped() {
        return invoiceId == null ? null : InvoiceId.of(invoiceId);
    }
}
