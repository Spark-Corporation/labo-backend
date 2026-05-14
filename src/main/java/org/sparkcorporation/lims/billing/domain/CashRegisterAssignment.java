package org.sparkcorporation.lims.billing.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Join entity carrying {@code (cash_register_id, user_id)} composite primary key plus the {@code
 * assigned_at} audit timestamp. Lives on the {@link CashRegister} aggregate via {@code @OneToMany}.
 *
 * <p>{@code user_id} references {@code iam.users.id} at the application layer only — there is no
 * physical foreign-key constraint, since {@code iam} is a sibling Modulith module. Existence is
 * validated by {@code DefaultCashRegisterApi.assignUsers} via {@code UserApi.existsById}.
 */
@Entity
@Table(name = "cash_register_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CashRegisterAssignment {

    @EmbeddedId private CashRegisterAssignmentKey id;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    public CashRegisterAssignment(String cashRegisterId, String userId, Instant assignedAt) {
        this.id = new CashRegisterAssignmentKey(cashRegisterId, userId);
        this.assignedAt = Objects.requireNonNull(assignedAt, "assignedAt");
    }

    public String cashRegisterId() {
        return id.cashRegisterId();
    }

    public String userId() {
        return id.userId();
    }

    /** Composite primary key {@code (cash_register_id, user_id)}. */
    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class CashRegisterAssignmentKey implements Serializable {

        @Column(name = "cash_register_id", length = 26, nullable = false)
        private String cashRegisterId;

        @Column(name = "user_id", length = 26, nullable = false)
        private String userId;

        public CashRegisterAssignmentKey(String cashRegisterId, String userId) {
            this.cashRegisterId = Objects.requireNonNull(cashRegisterId, "cashRegisterId");
            this.userId = Objects.requireNonNull(userId, "userId");
        }

        public String cashRegisterId() {
            return cashRegisterId;
        }

        public String userId() {
            return userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CashRegisterAssignmentKey other)) {
                return false;
            }
            return Objects.equals(cashRegisterId, other.cashRegisterId)
                    && Objects.equals(userId, other.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cashRegisterId, userId);
        }
    }
}
