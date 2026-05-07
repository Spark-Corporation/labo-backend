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
import jakarta.persistence.Version;

import org.hibernate.annotations.SQLDelete;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.ReferenceIntervalId;
import org.sparkcorporation.lims.catalog.api.ReferenceMode;
import org.sparkcorporation.lims.shared.domain.AgeUnit;
import org.sparkcorporation.lims.shared.domain.PatientCategory;
import org.sparkcorporation.lims.shared.domain.Sex;

/**
 * ReferenceInterval aggregate (per analysis × patient-category × age × sex × mode) per CDC §8
 * onglet "Intervalles de référence". JPA entity with soft delete via Hibernate {@link SQLDelete}.
 *
 * <p>NOTE: deliberately NO {@code @SQLRestriction} — the 1.5 result module needs {@code findById}
 * to resolve historical references even when soft-deleted. Repository methods explicitly filter
 * {@code deleted_at IS NULL} for active queries.
 *
 * <p>Mode-specific invariants (validated in {@link #create}):
 *
 * <ul>
 *   <li>{@link ReferenceMode#STANDARD} requires {@code unit1Min} and {@code unit1Max}; {@code
 *       unit1Min <= unit1Max}; {@code commentText} forbidden.
 *   <li>{@link ReferenceMode#COMMENT} requires non-blank {@code commentText}; numeric fields
 *       forbidden.
 *   <li>{@link ReferenceMode#NONE} forbids all data fields.
 * </ul>
 */
@Entity
@Table(name = "reference_intervals")
@SQLDelete(
        sql =
                "UPDATE reference_intervals SET deleted_at = now(), version = version + 1 WHERE id"
                        + " = ? AND version = ?")
public class ReferenceInterval {

    @Id
    @Column(length = 26)
    private String id;

    @Column(name = "analysis_id", nullable = false, length = 26)
    private String analysisId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private PatientCategory category;

    @Enumerated(EnumType.STRING)
    @Column(length = 1)
    @Nullable
    private Sex sex;

    @Column(name = "age_min")
    @Nullable
    private Integer ageMin;

    @Column(name = "age_max")
    @Nullable
    private Integer ageMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_unit", length = 10)
    @Nullable
    private AgeUnit ageUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReferenceMode mode;

    @Column(name = "unit_1_min", precision = 18, scale = 4)
    @Nullable
    private BigDecimal unit1Min;

    @Column(name = "unit_1_max", precision = 18, scale = 4)
    @Nullable
    private BigDecimal unit1Max;

    @Column(name = "unit_2_min", precision = 18, scale = 4)
    @Nullable
    private BigDecimal unit2Min;

    @Column(name = "unit_2_max", precision = 18, scale = 4)
    @Nullable
    private BigDecimal unit2Max;

    @Column(name = "comment_text", columnDefinition = "TEXT")
    @Nullable
    private String commentText;

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

    protected ReferenceInterval() {}

    public static ReferenceInterval create(
            NewReferenceInterval cmd, AnalysisId analysisId, Clock clock) {
        validateMode(cmd);
        var ri = new ReferenceInterval();
        ri.id = ReferenceIntervalId.generate().value();
        ri.analysisId = analysisId.value();
        ri.category = cmd.category();
        ri.sex = cmd.sex();
        ri.ageMin = cmd.ageMin();
        ri.ageMax = cmd.ageMax();
        ri.ageUnit = cmd.ageUnit();
        ri.mode = cmd.mode();
        ri.unit1Min = cmd.unit1Min();
        ri.unit1Max = cmd.unit1Max();
        ri.unit2Min = cmd.unit2Min();
        ri.unit2Max = cmd.unit2Max();
        ri.commentText = cmd.commentText();
        var now = Instant.now(clock);
        ri.createdAt = now;
        ri.updatedAt = now;
        return ri;
    }

    private static void validateMode(NewReferenceInterval cmd) {
        switch (cmd.mode()) {
            case STANDARD -> {
                if (cmd.unit1Min() == null || cmd.unit1Max() == null) {
                    throw new InvalidReferenceIntervalException(
                            "STANDARD mode requires unit1Min/unit1Max");
                }
                if (cmd.unit1Min().compareTo(cmd.unit1Max()) > 0) {
                    throw new InvalidReferenceIntervalException("unit1Min must be <= unit1Max");
                }
                if (cmd.commentText() != null) {
                    throw new InvalidReferenceIntervalException(
                            "STANDARD mode forbids commentText");
                }
            }
            case COMMENT -> {
                if (cmd.commentText() == null || cmd.commentText().isBlank()) {
                    throw new InvalidReferenceIntervalException(
                            "COMMENT mode requires commentText");
                }
                if (cmd.unit1Min() != null
                        || cmd.unit1Max() != null
                        || cmd.unit2Min() != null
                        || cmd.unit2Max() != null) {
                    throw new InvalidReferenceIntervalException(
                            "COMMENT mode forbids numeric fields");
                }
            }
            case NONE -> {
                if (cmd.unit1Min() != null
                        || cmd.unit1Max() != null
                        || cmd.unit2Min() != null
                        || cmd.unit2Max() != null
                        || cmd.commentText() != null) {
                    throw new InvalidReferenceIntervalException(
                            "NONE mode forbids all data fields");
                }
            }
        }
    }

    /** Clone this interval into a new category for the same analysis (replicate operation). */
    public ReferenceInterval cloneForCategory(PatientCategory newCategory, Clock clock) {
        var clone = new ReferenceInterval();
        clone.id = ReferenceIntervalId.generate().value();
        clone.analysisId = this.analysisId;
        clone.category = newCategory;
        clone.sex = this.sex;
        clone.ageMin = this.ageMin;
        clone.ageMax = this.ageMax;
        clone.ageUnit = this.ageUnit;
        clone.mode = this.mode;
        clone.unit1Min = this.unit1Min;
        clone.unit1Max = this.unit1Max;
        clone.unit2Min = this.unit2Min;
        clone.unit2Max = this.unit2Max;
        clone.commentText = this.commentText;
        var now = Instant.now(clock);
        clone.createdAt = now;
        clone.updatedAt = now;
        return clone;
    }

    /** Clone this interval onto a different analysis (used by Analysis duplicate). */
    public ReferenceInterval cloneForAnalysis(AnalysisId newAnalysisId, Clock clock) {
        var clone = new ReferenceInterval();
        clone.id = ReferenceIntervalId.generate().value();
        clone.analysisId = newAnalysisId.value();
        clone.category = this.category;
        clone.sex = this.sex;
        clone.ageMin = this.ageMin;
        clone.ageMax = this.ageMax;
        clone.ageUnit = this.ageUnit;
        clone.mode = this.mode;
        clone.unit1Min = this.unit1Min;
        clone.unit1Max = this.unit1Max;
        clone.unit2Min = this.unit2Min;
        clone.unit2Max = this.unit2Max;
        clone.commentText = this.commentText;
        var now = Instant.now(clock);
        clone.createdAt = now;
        clone.updatedAt = now;
        return clone;
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    public ReferenceIntervalId id() {
        return ReferenceIntervalId.of(id);
    }

    public String rawId() {
        return id;
    }

    public AnalysisId analysisId() {
        return AnalysisId.of(analysisId);
    }

    public String rawAnalysisId() {
        return analysisId;
    }

    public PatientCategory category() {
        return category;
    }

    public @Nullable Sex sex() {
        return sex;
    }

    public @Nullable Integer ageMin() {
        return ageMin;
    }

    public @Nullable Integer ageMax() {
        return ageMax;
    }

    public @Nullable AgeUnit ageUnit() {
        return ageUnit;
    }

    public ReferenceMode mode() {
        return mode;
    }

    public @Nullable BigDecimal unit1Min() {
        return unit1Min;
    }

    public @Nullable BigDecimal unit1Max() {
        return unit1Max;
    }

    public @Nullable BigDecimal unit2Min() {
        return unit2Min;
    }

    public @Nullable BigDecimal unit2Max() {
        return unit2Max;
    }

    public @Nullable String commentText() {
        return commentText;
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
