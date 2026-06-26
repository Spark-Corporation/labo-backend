package org.sparkcorporation.lims.result.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import org.sparkcorporation.lims.catalog.api.ReferenceIntervalSummary;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.result.api.ResultLineId;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * {@code ResultLine} aggregate-internal entity per spec §4.2. Polymorphic value persistence: 3
 * nullable typed columns ({@code value_integer / value_real / value_text}) with the result-type
 * discriminator. The sealed {@link ResultValue} domain projection is reconstructed in {@link
 * #rehydrateValue()} on load.
 *
 * <p>Per spec §4.2 / §9.1: a DB-level CHECK constraint ensures at most one of the three value
 * columns is populated at any time.
 *
 * <p>Row-level history (SP8): {@code @Audited} — every insert/update writes a {@code
 * result_lines_aud} row, giving per-line result-value history under the {@code Result} aggregate.
 * The {@code @ManyToOne result} association targets the audited {@code Result}, so its FK is
 * audited inline (no exclusion needed).
 */
@Entity
@Audited
@Table(name = "result_lines")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResultLine {

    @Id
    @Column(name = "id", length = 26, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false)
    private Result result;

    @Column(name = "component_analysis_id", length = 26, nullable = false)
    private String componentAnalysisId;

    @Column(name = "component_code_snapshot", length = 30, nullable = false)
    private String componentCodeSnapshot;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "result_type", length = 15, nullable = false)
    @Enumerated(EnumType.STRING)
    private ResultType resultType;

    @Column(name = "value_integer")
    private @Nullable Long valueInteger;

    @Column(name = "value_real", precision = 18, scale = 4)
    private @Nullable BigDecimal valueReal;

    @Column(name = "value_text", length = 2000)
    private @Nullable String valueText;

    @Column(name = "unit1_snapshot", length = 20)
    private @Nullable String unit1Snapshot;

    @Column(name = "unit1_decimals")
    private @Nullable Integer unit1Decimals;

    @Column(name = "unit2_snapshot", length = 20)
    private @Nullable String unit2Snapshot;

    @Column(name = "unit2_decimals")
    private @Nullable Integer unit2Decimals;

    @Column(name = "unit_1_to_2_factor", precision = 18, scale = 6)
    private @Nullable BigDecimal unit1To2Factor;

    @Column(name = "reference_interval_id", length = 26)
    private @Nullable String referenceIntervalId;

    @Column(name = "is_out_of_range", nullable = false)
    private boolean isOutOfRange;

    @Column(name = "comment_text", length = 2000)
    private @Nullable String commentText;

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

    /**
     * Reconstructed sealed-type projection — {@code @Transient}, populated in {@code @PostLoad}.
     */
    @Transient private @Nullable ResultValue resultValue;

    /**
     * Factory used by {@link Result#create} to materialize empty draft lines at parent creation
     * time. Package-private — only the aggregate root may produce lines.
     */
    static ResultLine draftFor(
            Result parent,
            String componentAnalysisId,
            String componentCodeSnapshot,
            int position,
            ResultType resultType,
            @Nullable String unit1,
            @Nullable Integer unit1Decimals,
            @Nullable String unit2,
            @Nullable Integer unit2Decimals,
            @Nullable BigDecimal conversionFactor) {
        ResultLine line = new ResultLine();
        line.id = Ulid.generate();
        line.result = parent;
        line.componentAnalysisId = componentAnalysisId;
        line.componentCodeSnapshot = componentCodeSnapshot;
        line.position = position;
        line.resultType = resultType;
        line.unit1Snapshot = unit1;
        line.unit1Decimals = unit1Decimals;
        line.unit2Snapshot = unit2;
        line.unit2Decimals = unit2Decimals;
        line.unit1To2Factor = conversionFactor;
        line.isOutOfRange = false;
        return line;
    }

    @PostLoad
    void rehydrateValue() {
        this.resultValue =
                switch (resultType) {
                    case INTEGER -> valueInteger != null ? new IntegerResult(valueInteger) : null;
                    case REAL -> valueReal != null ? new NumericResult(valueReal) : null;
                    case TEXT -> valueText != null ? new TextResult(valueText) : null;
                    case FORMULA -> valueReal != null ? new FormulaResult(valueReal) : null;
                    case FREE -> valueText != null ? new FreeFormResult(valueText) : null;
                };
        // Defensive: catches malformed rows where the populated value column does not match
        // resultType (e.g. resultType=REAL but only value_text is set).
        if (this.resultValue == null && hasAnyValueColumn()) {
            throw new IllegalStateException(
                    "ResultLine "
                            + id
                            + " resultType="
                            + resultType
                            + " inconsistent with populated value columns");
        }
    }

    private boolean hasAnyValueColumn() {
        return valueInteger != null || valueReal != null || valueText != null;
    }

    public boolean hasValue() {
        return hasAnyValueColumn();
    }

    /** Aggregate-internal mutator — only {@link Result#setLineValue} should call this. */
    void setValue(NewLineValue cmd, @Nullable ReferenceIntervalSummary matchedRi) {
        // SP11: FORMULA results are server-computed (see applyComputedFormula); operators must not
        // set them. Reject up front, before any column is touched.
        if (resultType == ResultType.FORMULA) {
            throw new FormulaLineNotEditableException(new ResultLineId(id));
        }
        // clear existing typed value first
        this.valueInteger = null;
        this.valueReal = null;
        this.valueText = null;
        if (cmd.value() == null) {
            this.referenceIntervalId = null;
            this.isOutOfRange = false;
            this.resultValue = null;
        } else {
            validateValueKindMatchesType(cmd.value());
            switch (cmd.value()) {
                case IntegerResult ir -> this.valueInteger = ir.value();
                case NumericResult nr -> this.valueReal = nr.value();
                case TextResult tr -> this.valueText = tr.value();
                // Unreachable: FORMULA lines are rejected above; a FormulaResult value can only
                // pair with a FORMULA line (enforced by validateValueKindMatchesType).
                case FormulaResult ignored ->
                        throw new FormulaLineNotEditableException(new ResultLineId(id));
                case FreeFormResult ff -> this.valueText = ff.value();
            }
            this.referenceIntervalId = matchedRi == null ? null : matchedRi.id().value();
            this.isOutOfRange =
                    ReferenceIntervalMatcher.isOutOfRange(
                            resultType, valueInteger, valueReal, valueText, matchedRi);
            this.resultValue = cmd.value();
        }
        if (cmd.commentText() != null) {
            this.commentText = cmd.commentText().isEmpty() ? null : cmd.commentText();
        }
    }

    /**
     * Aggregate-internal: sets a server-computed FORMULA value ({@code null} clears it). The
     * recompute path ({@code FormulaRecalculationService}) only — bypasses the operator-entry guard
     * in {@link #setValue}. Stores the value in {@code value_real} and recomputes {@code
     * isOutOfRange}.
     */
    void applyComputedFormula(
            @Nullable BigDecimal value, @Nullable ReferenceIntervalSummary matchedRi) {
        this.valueInteger = null;
        this.valueText = null;
        this.valueReal = value;
        if (value == null) {
            this.referenceIntervalId = null;
            this.isOutOfRange = false;
            this.resultValue = null;
        } else {
            this.referenceIntervalId = matchedRi == null ? null : matchedRi.id().value();
            this.isOutOfRange =
                    ReferenceIntervalMatcher.isOutOfRange(
                            ResultType.FORMULA, null, value, null, matchedRi);
            this.resultValue = new FormulaResult(value);
        }
    }

    /** Aggregate-internal mutator — only {@link Result#clearLineValue} should call this. */
    void clearValue() {
        this.valueInteger = null;
        this.valueReal = null;
        this.valueText = null;
        this.referenceIntervalId = null;
        this.isOutOfRange = false;
        this.resultValue = null;
    }

    private void validateValueKindMatchesType(ResultValue value) {
        boolean compatible =
                switch (resultType) {
                    case INTEGER -> value instanceof IntegerResult;
                    case REAL -> value instanceof NumericResult;
                    case TEXT -> value instanceof TextResult;
                    case FORMULA -> value instanceof FormulaResult;
                    case FREE -> value instanceof FreeFormResult;
                };
        if (!compatible) {
            throw new ResultLineKindMismatchException(
                    new ResultLineId(id), resultType, value.getClass().getSimpleName());
        }
    }

    public ResultLineId resultLineId() {
        return new ResultLineId(id);
    }
}
