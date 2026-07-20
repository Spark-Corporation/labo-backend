package org.sparkcorporation.lims.catalog.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.InsuranceId;

/**
 * Insurance (CDC §I line 23, "Boutons fenêtre" lines 265-274) — payer entity covering a fraction of
 * a patient's lab invoice. Catalog reference dictionary; cross-module reads via {@code
 * InsuranceApi}.
 *
 * <p>Soft-delete via {@code disabled_at}: {@link SQLRestriction} filters disabled rows from
 * standard finders. Cross-module FK integrity from {@code patients.insurance_id} and {@code
 * lab_files.insurance_id} survives via the snapshot-resolution policy (billing spec §4.2): the live
 * row is re-read lazily through {@code InsuranceApi.findById} which returns empty when disabled,
 * and snapshot fields on the referencing record carry the at-creation-time identity.
 *
 * <p>{@code code} is user-supplied (CDC line 268, varchar(8)) and immutable after create.
 */
@Entity
@Table(name = "insurances")
@SQLRestriction("disabled_at IS NULL")
public class Insurance {

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @Column(length = 8, nullable = false, unique = true)
    private String code;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(name = "responsible_email", length = 255)
    @Nullable
    private String responsibleEmail;

    @Column(name = "responsible_contact", length = 50)
    @Nullable
    private String responsibleContact;

    @Column(name = "b_rate", nullable = false, precision = 18, scale = 2)
    private BigDecimal bRate;

    @Column(name = "coverage_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal coverageRate;

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

    protected Insurance() {}

    public static Insurance create(NewInsurance req, Clock clock) {
        var i = new Insurance();
        i.id = InsuranceId.generate().value();
        i.code = req.code();
        i.name = req.name();
        i.responsibleEmail = req.responsibleEmail();
        i.responsibleContact = req.responsibleContact();
        i.bRate = req.bRate();
        i.coverageRate = req.coverageRate();
        var now = Instant.now(clock);
        i.createdAt = now;
        i.updatedAt = now;
        return i;
    }

    /** Null field on {@link UpdateInsurance} = "leave unchanged". {@code code} is immutable. */
    public void update(UpdateInsurance req, Clock clock) {
        if (req.name() != null) this.name = req.name();
        if (req.responsibleEmail() != null) this.responsibleEmail = req.responsibleEmail();
        if (req.responsibleContact() != null) this.responsibleContact = req.responsibleContact();
        if (req.bRate() != null) this.bRate = req.bRate();
        if (req.coverageRate() != null) this.coverageRate = req.coverageRate();
        this.updatedAt = Instant.now(clock);
    }

    public void disable(Clock clock) {
        var now = Instant.now(clock);
        this.disabledAt = now;
        this.updatedAt = now;
    }

    public boolean isDisabled() {
        return disabledAt != null;
    }

    public InsuranceId id() {
        return InsuranceId.of(id);
    }

    public String rawId() {
        return id;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public @Nullable String responsibleEmail() {
        return responsibleEmail;
    }

    public @Nullable String responsibleContact() {
        return responsibleContact;
    }

    public BigDecimal bRate() {
        return bRate;
    }

    public BigDecimal coverageRate() {
        return coverageRate;
    }

    public @Nullable Instant disabledAt() {
        return disabledAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
