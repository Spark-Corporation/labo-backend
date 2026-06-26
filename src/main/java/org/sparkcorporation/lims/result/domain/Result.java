package org.sparkcorporation.lims.result.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import org.sparkcorporation.lims.catalog.api.ReferenceIntervalSummary;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.result.api.AntibiogramReadingId;
import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.result.api.ResultLineId;
import org.sparkcorporation.lims.result.api.ResultState;
import org.sparkcorporation.lims.shared.domain.PatientCategory;
import org.sparkcorporation.lims.shared.domain.Sex;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * {@code Result} aggregate root per spec §4.1. One row per {@code AnalysisRequest}, eagerly created
 * via {@code AnalysisRequestAdded} (chunk 4). Soft-deleted via Hibernate {@link SQLDelete}.
 *
 * <p>Per spec §16.6 the two child collections are typed as {@code Set<...>} (with
 * {@code @OrderBy("position ASC")}) to avoid Hibernate's {@code MultipleBagFetchException} when
 * both are loaded together.
 *
 * <p>Per spec §9.3 / CLAUDE.md: NO {@code @SQLRestriction} on this aggregate. Repository methods
 * filter {@code deleted_at IS NULL} explicitly.
 *
 * <p>Row-level history (SP8): {@code @Audited} — every insert/update/soft-delete writes a {@code
 * results_aud} row. The {@code lines} one-to-many IS audited ({@code ResultLine} is itself
 * {@code @Audited}, giving per-line result-value history). The {@code antibiogramReadings}
 * one-to-many is {@code @NotAudited}: {@code AntibiogramReading} is not an audited entity (out of
 * SP8 scope), so the association must be excluded or Envers fails to bootstrap.
 */
@Entity
@Audited
@Table(name = "results")
@SQLDelete(
        sql =
                "UPDATE results SET deleted_at = now(), version = version + 1 WHERE id = ? AND"
                        + " version = ?")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Result {

    @Id
    @Column(name = "id", length = 26, nullable = false)
    private String id;

    @Column(name = "lab_file_id", length = 26, nullable = false)
    private String labFileId;

    @Column(name = "analysis_request_id", length = 26, nullable = false, unique = true)
    private String analysisRequestId;

    @Column(name = "analysis_id", length = 26, nullable = false)
    private String analysisId;

    @Column(name = "analysis_code_snapshot", length = 30, nullable = false)
    private String analysisCodeSnapshot;

    @Column(name = "patient_category_snapshot", length = 32, nullable = false)
    @Enumerated(EnumType.STRING)
    private PatientCategory patientCategorySnapshot;

    @Column(name = "patient_sex_snapshot", length = 1, nullable = false)
    @Enumerated(EnumType.STRING)
    private Sex patientSexSnapshot;

    @Column(name = "patient_age_in_days_snapshot")
    private @Nullable Integer patientAgeInDaysSnapshot;

    @Column(name = "state", length = 32, nullable = false)
    @Enumerated(EnumType.STRING)
    private ResultState state;

    @Column(name = "technical_validated_by", length = 26)
    private @Nullable String technicalValidatedBy;

    @Column(name = "technical_validated_at")
    private @Nullable Instant technicalValidatedAt;

    @Column(name = "biological_validated_by", length = 26)
    private @Nullable String biologicalValidatedBy;

    @Column(name = "biological_validated_at")
    private @Nullable Instant biologicalValidatedAt;

    @OneToMany(
            mappedBy = "result",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private Set<ResultLine> lines = new LinkedHashSet<>();

    @OneToMany(
            mappedBy = "result",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    @NotAudited
    private Set<AntibiogramReading> antibiogramReadings = new LinkedHashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", length = 26, updatable = false)
    private @Nullable String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 26)
    private @Nullable String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "deleted_at")
    private @Nullable Instant deletedAt;

    /**
     * Materializes a brand-new {@code Result} in {@link ResultState#DRAFT}. {@code SIMPLE} analyses
     * produce a single line keyed by the parent analysis itself; {@code GROUPED} and {@code
     * SOCIETY_PANEL} analyses iterate {@code cmd.components()} and produce one line per component
     * in {@code position} order.
     */
    public static Result create(NewResult cmd, Clock clock) {
        Result r = new Result();
        r.id = Ulid.generate();
        r.labFileId = cmd.labFileId().value();
        r.analysisRequestId = cmd.analysisRequestId().value();
        r.analysisId = cmd.analysisId().value();
        r.analysisCodeSnapshot = cmd.analysisCodeSnapshot();
        r.patientCategorySnapshot = cmd.patientCategory();
        r.patientSexSnapshot = cmd.patientSex();
        r.patientAgeInDaysSnapshot = cmd.patientAgeInDays();
        r.state = ResultState.DRAFT;
        Instant now = clock.instant();
        r.createdAt = now;
        r.updatedAt = now;
        switch (cmd.analysisKind()) {
            case SIMPLE ->
                    r.lines.add(
                            ResultLine.draftFor(
                                    r,
                                    cmd.analysisId().value(),
                                    cmd.analysisCodeSnapshot(),
                                    0,
                                    cmd.resultType(),
                                    cmd.unit1(),
                                    cmd.unit1Decimals(),
                                    cmd.unit2(),
                                    cmd.unit2Decimals(),
                                    cmd.conversionFactor()));
            case GROUPED, SOCIETY_PANEL -> {
                int pos = 0;
                for (var comp : cmd.components()) {
                    r.lines.add(
                            ResultLine.draftFor(
                                    r,
                                    comp.childId().value(),
                                    comp.childCode(),
                                    pos++,
                                    cmd.resultType(),
                                    cmd.unit1(),
                                    cmd.unit1Decimals(),
                                    cmd.unit2(),
                                    cmd.unit2Decimals(),
                                    cmd.conversionFactor()));
                }
            }
        }
        return r;
    }

    /** Sets the value on the named line; recomputes parent state from line completeness. */
    public void setLineValue(
            ResultLineId lineId, NewLineValue cmd, @Nullable ReferenceIntervalSummary matchedRi) {
        guardNotLocked();
        ResultLine line = findLine(lineId);
        line.setValue(cmd, matchedRi);
        recomputeState();
    }

    /** Clears the value on the named line; may regress {@code ENTERED → DRAFT}. */
    public void clearLineValue(ResultLineId lineId) {
        guardNotLocked();
        ResultLine line = findLine(lineId);
        line.clearValue();
        recomputeState();
    }

    /** Lines whose result type is FORMULA (server-computed; SP11). Read-only snapshot. */
    public List<ResultLine> formulaLines() {
        return lines.stream().filter(l -> l.getResultType() == ResultType.FORMULA).toList();
    }

    /**
     * Resolved numeric values of <strong>measured</strong> (non-FORMULA) lines, keyed by component
     * code. Skips FORMULA, null, and non-numeric (TEXT/FREE) lines — formula codes are fed into the
     * recompute map only as each resolves, so a stale prior-transaction formula value never seeds a
     * recompute. INTEGER widens to {@link BigDecimal}.
     */
    public Map<String, BigDecimal> measuredNumericValues() {
        var values = new LinkedHashMap<String, BigDecimal>();
        for (ResultLine l : lines) {
            if (l.getResultType() == ResultType.FORMULA) {
                continue;
            }
            BigDecimal v =
                    switch (l.getResultType()) {
                        case INTEGER ->
                                l.getValueInteger() == null
                                        ? null
                                        : BigDecimal.valueOf(l.getValueInteger());
                        case REAL -> l.getValueReal();
                        case TEXT, FREE, FORMULA -> null;
                    };
            if (v != null) {
                values.put(l.getComponentCodeSnapshot(), v);
            }
        }
        return values;
    }

    /**
     * Sets a server-computed FORMULA value on the named line ({@code null} clears it) and
     * recomputes parent state. Recompute path only (see {@code FormulaRecalculationService}).
     */
    public void applyFormula(
            ResultLineId lineId,
            @Nullable BigDecimal value,
            @Nullable ReferenceIntervalSummary ri) {
        guardNotLocked();
        ResultLine line = findLine(lineId);
        line.applyComputedFormula(value, ri);
        recomputeState();
    }

    public AntibiogramReading addAntibiogramReading(NewAntibiogramReading cmd) {
        guardNotLocked();
        int nextPos = antibiogramReadings.size();
        AntibiogramReading ar = AntibiogramReading.create(this, cmd, nextPos);
        antibiogramReadings.add(ar);
        return ar;
    }

    public void removeAntibiogramReading(AntibiogramReadingId readingId) {
        guardNotLocked();
        boolean removed = antibiogramReadings.removeIf(ar -> ar.getId().equals(readingId.value()));
        if (!removed) {
            throw new AntibiogramReadingNotFoundException(getResultIdTyped(), readingId);
        }
    }

    public void validateTechnical(String userId, Clock clock) {
        if (state != ResultState.ENTERED) {
            throw new IllegalStateTransitionException(
                    getResultIdTyped(), state, ResultState.TECHNICAL_VALIDATED);
        }
        state = ResultState.TECHNICAL_VALIDATED;
        technicalValidatedBy = userId;
        technicalValidatedAt = clock.instant();
    }

    public void validateBiological(String userId, Clock clock) {
        if (state != ResultState.TECHNICAL_VALIDATED) {
            throw new IllegalStateTransitionException(
                    getResultIdTyped(), state, ResultState.BIOLOGICAL_VALIDATED);
        }
        state = ResultState.BIOLOGICAL_VALIDATED;
        biologicalValidatedBy = userId;
        biologicalValidatedAt = clock.instant();
    }

    /**
     * Pre-condition for repository delete. Hibernate's {@link SQLDelete} performs the actual
     * UPDATE; this method exists so callers (services, event handlers) can short-circuit on a
     * non-DRAFT aggregate before invoking the repository.
     */
    public void delete() {
        if (state != ResultState.DRAFT) {
            throw new ResultNotDeletableInStateException(getResultIdTyped(), state);
        }
    }

    public ResultId resultId() {
        return getResultIdTyped();
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    private void recomputeState() {
        if (state == ResultState.TECHNICAL_VALIDATED || state == ResultState.BIOLOGICAL_VALIDATED) {
            return;
        }
        boolean allFilled = !lines.isEmpty() && lines.stream().allMatch(ResultLine::hasValue);
        state = allFilled ? ResultState.ENTERED : ResultState.DRAFT;
    }

    private void guardNotLocked() {
        if (state == ResultState.TECHNICAL_VALIDATED || state == ResultState.BIOLOGICAL_VALIDATED) {
            throw new ResultLockedException(getResultIdTyped(), state);
        }
    }

    private ResultLine findLine(ResultLineId lineId) {
        return lines.stream()
                .filter(l -> l.getId().equals(lineId.value()))
                .findFirst()
                .orElseThrow(() -> new ResultLineNotFoundException(getResultIdTyped(), lineId));
    }

    private ResultId getResultIdTyped() {
        return new ResultId(id);
    }
}
