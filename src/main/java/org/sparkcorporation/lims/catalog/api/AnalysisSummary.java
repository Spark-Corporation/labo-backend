package org.sparkcorporation.lims.catalog.api;

import java.math.BigDecimal;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;

/**
 * Cross-module read snapshot of the {@code Analysis} aggregate.
 *
 * <p>Embeds flat snapshots of the linked Discipline / Matrix / Recipient (avoids cross-module N+1)
 * plus the Units VO, AnalyticalDelay VO, components, and techniques in a single shape.
 */
public record AnalysisSummary(
        AnalysisId id,
        String code,
        String name,
        AnalysisKind kind,
        BigDecimal indiciaryValueB,
        int labelCount,
        DisciplineSnapshot discipline,
        @Nullable MatrixSnapshot matrix,
        @Nullable RecipientSnapshot recipient,
        ResultType resultType,
        @Nullable String interpretationText,
        @Nullable String defaultPortoir,
        @Nullable Integer defaultConservationDays,
        UnitsSummary units,
        AnalyticalDelaySummary delay,
        List<ComponentLink> components,
        List<TechniqueLink> techniques) {

    /** Flat snapshot of the linked discipline at read time — never null (FK NOT NULL). */
    public record DisciplineSnapshot(
            DisciplineId id, String code, String name, DisciplinePrefix prefix) {}

    /** Flat snapshot of the linked matrix at read time. */
    public record MatrixSnapshot(MatrixId id, String code, String name) {}

    /** Flat snapshot of the linked recipient at read time. */
    public record RecipientSnapshot(RecipientId id, String code, String name) {}

    /** Embedded Units VO read shape. */
    public record UnitsSummary(
            @Nullable String unit1,
            @Nullable Integer unit1Decimals,
            @Nullable String unit2,
            @Nullable Integer unit2Decimals,
            @Nullable BigDecimal conversionFactor) {}

    /** Embedded AnalyticalDelay VO read shape. */
    public record AnalyticalDelaySummary(
            @Nullable Integer days, @Nullable Integer hours, @Nullable Integer anteriorityCount) {}

    /** Per-row link from a GROUPED/SOCIETY_PANEL parent to a SIMPLE child. */
    public record ComponentLink(
            AnalysisId childId, String childCode, String childName, int position) {}

    /** Per-row link from an analysis to one of its techniques (with default flag). */
    public record TechniqueLink(TechniqueId id, String code, String name, boolean isDefault) {}
}
