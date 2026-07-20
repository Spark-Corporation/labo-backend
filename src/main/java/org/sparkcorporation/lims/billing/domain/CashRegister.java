package org.sparkcorporation.lims.billing.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.api.CashRegisterCode;
import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Cash-register aggregate (CDC §VIII, spec §5.3) — a logical till owned by one or more cashiers.
 * The Payment aggregate (chunk 7) links to a cash register via {@code payments.cash_register_id};
 * the assigned-user set gates "only the assigned cashier may collect through this till".
 *
 * <p>Soft-disable via {@code disabled_at} + {@link SQLRestriction} hides disabled registers from
 * standard finders (search bypasses via native SQL).
 *
 * <p>{@code code} (2 alphanumeric chars) and {@code sequenceNumber} are immutable after creation.
 * {@code label} is the only field exposed by {@link #update(UpdateCashRegister, Clock)}.
 */
@Entity
@Table(name = "cash_registers")
@SQLRestriction("disabled_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CashRegister {

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @Column(name = "code", length = 2, nullable = false, unique = true, updatable = false)
    private String code;

    @Column(name = "label", length = 255, nullable = false)
    private String label;

    @Column(name = "sequence_number", nullable = false, unique = true, updatable = false)
    private Integer sequenceNumber;

    @Column(name = "disabled_at")
    @Nullable
    private Instant disabledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 26)
    @Nullable
    private String createdBy;

    @Column(name = "updated_by", length = 26)
    @Nullable
    private String updatedBy;

    @Version private long version;

    @OneToMany(
            mappedBy = "id.cashRegisterId",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    private Set<CashRegisterAssignment> assignments = new HashSet<>();

    /** Construct a fresh aggregate. Validates {@code code} via {@link CashRegisterCode}. */
    public static CashRegister create(NewCashRegister req, Clock clock) {
        // Side-effect: validates the code shape, throws IllegalArgumentException if malformed.
        CashRegisterCode.of(req.code());

        var cr = new CashRegister();
        cr.id = Ulid.generate();
        cr.code = req.code();
        cr.label = req.label();
        cr.sequenceNumber = req.sequenceNumber();
        var now = Instant.now(clock);
        cr.createdAt = now;
        cr.updatedAt = now;
        return cr;
    }

    /**
     * Mutate {@code label}; ignored for null. {@code code} and {@code sequenceNumber} are fixed.
     */
    public void update(UpdateCashRegister req, Clock clock) {
        if (req.label() != null) {
            this.label = req.label();
        }
        this.updatedAt = Instant.now(clock);
    }

    /** Soft-disable. Idempotent: a second call is a no-op aside from {@code updatedAt}. */
    public void disable(Clock clock) {
        var now = Instant.now(clock);
        if (this.disabledAt == null) {
            this.disabledAt = now;
        }
        this.updatedAt = now;
    }

    /** Re-enable a previously soft-disabled register (symmetric with {@link #disable}). */
    public void enable(Clock clock) {
        this.disabledAt = null;
        this.updatedAt = Instant.now(clock);
    }

    public boolean isDisabled() {
        return disabledAt != null;
    }

    /**
     * Replace the assigned-user set: removes assignments not in {@code newUserIds} and adds new
     * ones with {@code assigned_at = clock.instant()}. Preserves existing assignments unchanged.
     */
    public void replaceAssignments(Set<UserId> newUserIds, Clock clock) {
        var now = Instant.now(clock);
        var targetIds =
                newUserIds.stream().map(UserId::value).collect(java.util.stream.Collectors.toSet());
        this.assignments.removeIf(a -> !targetIds.contains(a.userId()));
        var existingIds = new java.util.HashSet<String>();
        for (var a : this.assignments) {
            existingIds.add(a.userId());
        }
        for (UserId uid : newUserIds) {
            if (!existingIds.contains(uid.value())) {
                this.assignments.add(new CashRegisterAssignment(this.id, uid.value(), now));
            }
        }
        this.updatedAt = now;
    }

    /** Predicate used by Payment to gate collection authorization. */
    public boolean isUserAssigned(String userId) {
        for (var a : this.assignments) {
            if (a.userId().equals(userId)) {
                return true;
            }
        }
        return false;
    }

    public CashRegisterId cashRegisterId() {
        return CashRegisterId.of(id);
    }

    /** Snapshot of the current assigned-user ULIDs. */
    public Set<String> userIds() {
        return assignments.stream()
                .map(CashRegisterAssignment::userId)
                .collect(Collectors.toUnmodifiableSet());
    }
}
