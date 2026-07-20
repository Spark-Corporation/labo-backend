package org.sparkcorporation.lims.catalog.domain;

import java.time.Clock;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.SQLDelete;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.PrescriberCode;
import org.sparkcorporation.lims.catalog.api.PrescriberId;

/**
 * Prescriber (CDC §49 + §261-263) aggregate. JPA entity with soft delete via Hibernate {@link
 * SQLDelete}.
 *
 * <p>NOTE: deliberately NO {@code @SQLRestriction} — historical FK integrity from 1.4 {@code
 * order.lab_files} requires {@code findById} to return rows even when {@code deleted_at} is set.
 * Repository methods explicitly filter {@code deleted_at IS NULL} for active queries.
 *
 * <p>{@code code} is server-generated as {@code PRS-NNNNNN} by {@code CatalogCodeGenerator} and
 * passed in to {@link #create(NewPrescriber, String, Specialty, Clock)}; it is immutable
 * thereafter.
 */
@Entity
@Table(name = "prescribers")
@SQLDelete(
        sql =
                "UPDATE prescribers SET deleted_at = now(), version = version + 1 WHERE id = ? AND"
                        + " version = ?")
public class Prescriber {

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @Column(length = 30, nullable = false, unique = true)
    private String code;

    @Column(length = 10)
    @Nullable
    private String title;

    @Column(length = 120, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialty_id")
    @Nullable
    private Specialty specialty;

    @Column(length = 30)
    @Nullable
    private String phone;

    @Column(length = 120)
    @Nullable
    private String email;

    @Column(name = "license_number", length = 40)
    @Nullable
    private String licenseNumber;

    @Column(length = 120)
    @Nullable
    private String institution;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "deleted_at")
    @Nullable
    private Instant deletedAt;

    protected Prescriber() {}

    /** {@code code} is server-generated ({@code CatalogCodeGenerator}) and passed in. */
    public static Prescriber create(
            NewPrescriber cmd, String code, @Nullable Specialty specialty, Clock clock) {
        var p = new Prescriber();
        p.id = PrescriberId.generate().value();
        p.code = PrescriberCode.of(code).value();
        p.title = cmd.title();
        p.name = cmd.name();
        p.specialty = specialty;
        p.phone = cmd.phone();
        p.email = cmd.email();
        p.licenseNumber = cmd.licenseNumber();
        p.institution = cmd.institution();
        var now = Instant.now(clock);
        p.createdAt = now;
        p.updatedAt = now;
        return p;
    }

    /**
     * Update fields; {@code null} on a primitive field means "leave unchanged". The {@code
     * newSpecialty} argument is resolved by the service from {@code cmd.specialtyId()} and passed
     * in only when a specialty change is intended; pass {@code null} to keep the existing
     * specialty.
     */
    public void update(UpdatePrescriber cmd, @Nullable Specialty newSpecialty, Clock clock) {
        if (cmd.title() != null) this.title = cmd.title();
        if (cmd.name() != null) this.name = cmd.name();
        if (cmd.phone() != null) this.phone = cmd.phone();
        if (cmd.email() != null) this.email = cmd.email();
        if (cmd.licenseNumber() != null) this.licenseNumber = cmd.licenseNumber();
        if (cmd.institution() != null) this.institution = cmd.institution();
        if (newSpecialty != null) this.specialty = newSpecialty;
        this.updatedAt = Instant.now(clock);
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    public PrescriberId id() {
        return PrescriberId.of(id);
    }

    public String rawId() {
        return id;
    }

    public String code() {
        return code;
    }

    public @Nullable String title() {
        return title;
    }

    public String name() {
        return name;
    }

    public @Nullable Specialty specialty() {
        return specialty;
    }

    public @Nullable String phone() {
        return phone;
    }

    public @Nullable String email() {
        return email;
    }

    public @Nullable String licenseNumber() {
        return licenseNumber;
    }

    public @Nullable String institution() {
        return institution;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }

    public @Nullable Instant deletedAt() {
        return deletedAt;
    }
}
