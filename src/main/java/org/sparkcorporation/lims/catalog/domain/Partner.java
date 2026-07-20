package org.sparkcorporation.lims.catalog.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.PartnerId;
import org.sparkcorporation.lims.catalog.api.PartnerKind;

/**
 * Partner (CDC §I line 23, line 277) — corporate or institutional account that bills the entire
 * patient invoice (no patient share), in contrast to {@link Insurance}. Three kinds: SOCIETY,
 * PARTNER_LAB, HEALTH_CENTER.
 *
 * <p>Soft-delete via {@code disabled_at} with {@link SQLRestriction}; cross-module FK integrity
 * survives via the snapshot-resolution policy (billing spec §4.3).
 */
@Entity
@Table(name = "partners")
@SQLRestriction("disabled_at IS NULL")
public class Partner {

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @Column(length = 8, nullable = false, unique = true)
    private String code;

    @Column(length = 255, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PartnerKind kind;

    @Column(name = "responsible_email", length = 255)
    @Nullable
    private String responsibleEmail;

    @Column(name = "responsible_contact", length = 50)
    @Nullable
    private String responsibleContact;

    @Column(name = "b_rate", nullable = false, precision = 18, scale = 2)
    private BigDecimal bRate;

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

    protected Partner() {}

    public static Partner create(NewPartner req, Clock clock) {
        var p = new Partner();
        p.id = PartnerId.generate().value();
        p.code = req.code();
        p.name = req.name();
        p.kind = req.kind();
        p.responsibleEmail = req.responsibleEmail();
        p.responsibleContact = req.responsibleContact();
        p.bRate = req.bRate();
        var now = Instant.now(clock);
        p.createdAt = now;
        p.updatedAt = now;
        return p;
    }

    public void update(UpdatePartner req, Clock clock) {
        if (req.name() != null) this.name = req.name();
        if (req.kind() != null) this.kind = req.kind();
        if (req.responsibleEmail() != null) this.responsibleEmail = req.responsibleEmail();
        if (req.responsibleContact() != null) this.responsibleContact = req.responsibleContact();
        if (req.bRate() != null) this.bRate = req.bRate();
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

    public PartnerId id() {
        return PartnerId.of(id);
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

    public PartnerKind kind() {
        return kind;
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
