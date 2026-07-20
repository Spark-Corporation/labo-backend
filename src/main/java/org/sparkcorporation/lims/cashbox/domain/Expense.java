package org.sparkcorporation.lims.cashbox.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.billing.api.PaymentMode;
import org.sparkcorporation.lims.cashbox.api.ExpenseCategoryId;
import org.sparkcorporation.lims.cashbox.api.ExpenseId;
import org.sparkcorporation.lims.iam.api.UserId;

@Entity
@Table(name = "cashbox_expenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense {

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @Column(name = "category_id", nullable = false, length = 26)
    private String categoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentMode mode;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column private String description;

    @Column(name = "justification_scan_uri")
    private String justificationScanUri;

    @Column(name = "cash_register_id", nullable = false, length = 26)
    private String cashRegisterId;

    @Column(name = "recorded_by", nullable = false, length = 26)
    private String recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 26)
    private String createdBy;

    @Column(name = "updated_by", length = 26)
    private String updatedBy;

    @Version private long version;

    public static Expense record(
            ExpenseCategoryId category,
            PaymentMode mode,
            BigDecimal amount,
            String description,
            @Nullable String justificationScanUri,
            CashRegisterId cashRegisterId,
            UserId recordedBy,
            Clock clock) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
        var now = Instant.now(clock);
        var e = new Expense();
        e.id = ExpenseId.generate().value();
        e.categoryId = category.value();
        e.mode = mode;
        e.amount = amount;
        e.description = description;
        e.justificationScanUri = justificationScanUri;
        e.cashRegisterId = cashRegisterId.value();
        e.recordedBy = recordedBy.value();
        e.recordedAt = now;
        e.createdAt = now;
        e.updatedAt = now;
        e.createdBy = recordedBy.value();
        e.updatedBy = recordedBy.value();
        return e;
    }

    public ExpenseId expenseId() {
        return new ExpenseId(id);
    }

    /**
     * SP6 (spec §5): points this expense at its uploaded justification scan. Overwrite-allowed —
     * re-uploading replaces the pointer; the prior attachment row survives independently. The
     * {@code @Version} field bumps on flush.
     */
    public void attachJustificationScan(String storageUri, UserId by, Instant now) {
        this.justificationScanUri = storageUri;
        this.updatedAt = now;
        this.updatedBy = by.value();
    }
}
