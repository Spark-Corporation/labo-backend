package org.sparkcorporation.lims.result.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Web-tier mirror of {@link org.sparkcorporation.lims.result.api.ResultSummary}. Carries flat
 * snapshots plus the two child collections.
 */
public record ResultResponse(
        String id,
        String labFileId,
        String analysisRequestId,
        String analysisId,
        String analysisCodeSnapshot,
        String state,
        @Nullable String technicalValidatedBy,
        @Nullable Instant technicalValidatedAt,
        @Nullable String biologicalValidatedBy,
        @Nullable Instant biologicalValidatedAt,
        List<LineResponse> lines,
        List<AntibiogramReadingResponse> antibiogramReadings,
        Instant createdAt,
        @Nullable String createdByUserId) {

    public ResultResponse {
        lines = List.copyOf(lines);
        antibiogramReadings = List.copyOf(antibiogramReadings);
    }

    /** Web-tier mirror of {@code ResultSummary.LineSummary}. */
    public record LineResponse(
            String id,
            String componentAnalysisId,
            String componentCodeSnapshot,
            int position,
            String resultType,
            @Nullable Long valueInteger,
            @Nullable BigDecimal valueReal,
            @Nullable String valueText,
            @Nullable String unit1Snapshot,
            @Nullable Integer unit1Decimals,
            @Nullable String unit2Snapshot,
            @Nullable Integer unit2Decimals,
            @Nullable String referenceIntervalId,
            boolean isOutOfRange,
            @Nullable String commentText) {}

    /** Web-tier mirror of {@code ResultSummary.AntibiogramReadingSummary}. */
    public record AntibiogramReadingResponse(
            String id,
            String antibioticId,
            String measuredDiameter,
            String interpretedSensitivity,
            int position) {}
}
