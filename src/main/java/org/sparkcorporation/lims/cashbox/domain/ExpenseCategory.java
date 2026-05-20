package org.sparkcorporation.lims.cashbox.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.sparkcorporation.lims.cashbox.api.ExpenseCategoryId;
import org.sparkcorporation.lims.iam.api.UserId;

/**
 * Admin-configurable expense classification used by {@code cashbox_expenses.category_id}. Code is
 * uppercase-normalized and immutable after creation; label and active flag are mutable via the
 * dedicated methods. Soft-delete via {@code active = false} (no row removal — preserves historical
 * expense references).
 */
@Entity
@Table(name = "cashbox_expense_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseCategory {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9_-]{1,32}$");
    private static final int LABEL_MAX_LENGTH = 200;

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 26)
    private String createdBy;

    @Column(name = "updated_by", length = 26)
    private String updatedBy;

    @Version private long version;

    public static ExpenseCategory create(String code, String label, UserId createdBy, Clock clock) {
        validateCode(code);
        validateLabel(label);
        var now = Instant.now(clock);
        var c = new ExpenseCategory();
        c.id = ExpenseCategoryId.generate().value();
        c.code = code.toUpperCase();
        c.label = label.strip();
        c.active = true;
        c.createdAt = now;
        c.updatedAt = now;
        c.createdBy = createdBy.value();
        c.updatedBy = createdBy.value();
        return c;
    }

    public ExpenseCategoryId expenseCategoryId() {
        return new ExpenseCategoryId(id);
    }

    public void updateLabel(String newLabel, UserId by, Clock clock) {
        validateLabel(newLabel);
        this.label = newLabel.strip();
        this.updatedAt = Instant.now(clock);
        this.updatedBy = by.value();
    }

    public void setActive(boolean active, UserId by, Clock clock) {
        if (this.active == active) {
            return;
        }
        this.active = active;
        this.updatedAt = Instant.now(clock);
        this.updatedBy = by.value();
    }

    private static void validateCode(String code) {
        if (code == null || !CODE_PATTERN.matcher(code.toUpperCase()).matches()) {
            throw new IllegalArgumentException("code must match [A-Z0-9_-]{1,32}: " + code);
        }
    }

    private static void validateLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        if (label.length() > LABEL_MAX_LENGTH) {
            throw new IllegalArgumentException("label must be <= " + LABEL_MAX_LENGTH + " chars");
        }
    }
}
