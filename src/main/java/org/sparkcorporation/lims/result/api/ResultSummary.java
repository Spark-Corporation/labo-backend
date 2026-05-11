package org.sparkcorporation.lims.result.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AntibioticId;
import org.sparkcorporation.lims.catalog.api.ReferenceIntervalId;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.shared.domain.Sensitivity;

/**
 * Cross-module read snapshot of the {@code Result} aggregate. Carries flat values plus two child
 * collections ({@link LineSummary}, {@link AntibiogramReadingSummary}) per spec §3.
 */
public record ResultSummary(
        ResultId id,
        LabFileId labFileId,
        AnalysisRequestId analysisRequestId,
        AnalysisId analysisId,
        String analysisCodeSnapshot,
        ResultState state,
        @Nullable String technicalValidatedBy,
        @Nullable Instant technicalValidatedAt,
        @Nullable String biologicalValidatedBy,
        @Nullable Instant biologicalValidatedAt,
        List<LineSummary> lines,
        List<AntibiogramReadingSummary> antibiogramReadings,
        Instant createdAt,
        @Nullable String createdByUserId) {

    public ResultSummary {
        lines = List.copyOf(lines);
        antibiogramReadings = List.copyOf(antibiogramReadings);
    }

    /**
     * One line per analysis component, with polymorphic typed value + denormalized out-of-range.
     */
    public record LineSummary(
            ResultLineId id,
            AnalysisId componentAnalysisId,
            String componentCodeSnapshot,
            int position,
            ResultType resultType,
            @Nullable Long valueInteger,
            @Nullable BigDecimal valueReal,
            @Nullable String valueText,
            @Nullable String unit1Snapshot,
            @Nullable Integer unit1Decimals,
            @Nullable String unit2Snapshot,
            @Nullable Integer unit2Decimals,
            @Nullable ReferenceIntervalId referenceIntervalId,
            boolean isOutOfRange,
            @Nullable String commentText) {}

    /** One reading per antibiotic tested; {@code interpretedSensitivity} denormalized at save. */
    public record AntibiogramReadingSummary(
            AntibiogramReadingId id,
            AntibioticId antibioticId,
            BigDecimal measuredDiameter,
            Sensitivity interpretedSensitivity,
            int position) {}
}
