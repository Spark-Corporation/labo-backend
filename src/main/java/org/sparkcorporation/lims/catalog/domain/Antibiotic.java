package org.sparkcorporation.lims.catalog.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.SQLDelete;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import org.sparkcorporation.lims.catalog.api.AntibioticId;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Antibiotic aggregate. JPA entity with soft delete via Hibernate {@link SQLDelete}.
 *
 * <p>NOTE: deliberately NO {@code @SQLRestriction} — historical FK integrity from 1.3.b's Analysis
 * aggregate is preserved. Repository methods explicitly filter {@code deleted_at IS NULL}.
 *
 * <p>Invariant: when both {@code intermediateLower} and {@code intermediateUpper} are provided,
 * {@code intermediateLower ≤ intermediateUpper}. Used by 1.3.b's antibiogram interpreter.
 */
@Entity
@Table(name = "antibiotics")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(
        sql =
                "UPDATE antibiotics SET deleted_at = now(), version = version + 1 WHERE id = ? AND"
                        + " version = ?")
public class Antibiotic {

    private static final Pattern CODE_FORMAT = Pattern.compile("^[A-Z][A-Z0-9_]{1,29}$");

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @Column(length = 30, nullable = false, unique = true)
    private String code;

    @Column(length = 120, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    @Nullable
    private String description;

    @Column(length = 80)
    @Nullable
    private String family;

    @Column(name = "reference_diameter", precision = 5, scale = 2, nullable = false)
    private BigDecimal referenceDiameter;

    @Column(name = "intermediate_lower", precision = 5, scale = 2)
    @Nullable
    private BigDecimal intermediateLower;

    @Column(name = "intermediate_upper", precision = 5, scale = 2)
    @Nullable
    private BigDecimal intermediateUpper;

    @Column(name = "deleted_at")
    @Nullable
    private Instant deletedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", length = 26)
    @Nullable
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 26)
    @Nullable
    private String updatedBy;

    @Version private int version;

    protected Antibiotic() {}

    public static Antibiotic create(
            String code,
            String name,
            @Nullable String description,
            @Nullable String family,
            BigDecimal referenceDiameter,
            @Nullable BigDecimal intermediateLower,
            @Nullable BigDecimal intermediateUpper) {
        if (code == null || !CODE_FORMAT.matcher(code).matches()) {
            throw new IllegalArgumentException("invalid code: " + code);
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(referenceDiameter, "referenceDiameter");
        if (referenceDiameter.signum() < 0) {
            throw new IllegalArgumentException("referenceDiameter must be non-negative");
        }
        validateIntermediatePair(intermediateLower, intermediateUpper);

        var a = new Antibiotic();
        a.id = Ulid.generate();
        a.code = code;
        a.name = name.trim();
        a.description = description;
        a.family = family;
        a.referenceDiameter = referenceDiameter;
        a.intermediateLower = intermediateLower;
        a.intermediateUpper = intermediateUpper;
        return a;
    }

    public void update(
            @Nullable String name,
            @Nullable String description,
            @Nullable String family,
            @Nullable BigDecimal referenceDiameter,
            @Nullable BigDecimal intermediateLower,
            @Nullable BigDecimal intermediateUpper) {
        if (name != null) {
            if (name.isBlank()) throw new IllegalArgumentException("name must not be blank");
            this.name = name.trim();
        }
        if (description != null) this.description = description;
        if (family != null) this.family = family;
        if (referenceDiameter != null) {
            if (referenceDiameter.signum() < 0) {
                throw new IllegalArgumentException("referenceDiameter must be non-negative");
            }
            this.referenceDiameter = referenceDiameter;
        }
        BigDecimal newLower =
                intermediateLower != null ? intermediateLower : this.intermediateLower;
        BigDecimal newUpper =
                intermediateUpper != null ? intermediateUpper : this.intermediateUpper;
        validateIntermediatePair(newLower, newUpper);
        if (intermediateLower != null) this.intermediateLower = intermediateLower;
        if (intermediateUpper != null) this.intermediateUpper = intermediateUpper;
    }

    private static void validateIntermediatePair(
            @Nullable BigDecimal lower, @Nullable BigDecimal upper) {
        if (lower != null && upper != null && lower.compareTo(upper) > 0) {
            throw new IllegalArgumentException("intermediateLower must be ≤ intermediateUpper");
        }
    }

    public AntibioticId id() {
        return new AntibioticId(id);
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

    public @Nullable String description() {
        return description;
    }

    public @Nullable String family() {
        return family;
    }

    public BigDecimal referenceDiameter() {
        return referenceDiameter;
    }

    public @Nullable BigDecimal intermediateLower() {
        return intermediateLower;
    }

    public @Nullable BigDecimal intermediateUpper() {
        return intermediateUpper;
    }

    public @Nullable Instant deletedAt() {
        return deletedAt;
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public int version() {
        return version;
    }
}
