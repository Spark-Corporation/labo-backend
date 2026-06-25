package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.ResultType;

/**
 * Response DTO mirroring {@link org.sparkcorporation.lims.catalog.api.AnalysisSummary} with audit
 * fields ({@code createdAt}, {@code updatedAt}, {@code version}) added.
 */
public record AnalysisResponse(
        String id,
        String code,
        String name,
        AnalysisKind kind,
        BigDecimal indiciaryValueB,
        int labelCount,
        DisciplineSnapshotResponse discipline,
        @Nullable MatrixSnapshotResponse matrix,
        @Nullable RecipientSnapshotResponse recipient,
        ResultType resultType,
        @Nullable String interpretationText,
        @Nullable String formulaExpression,
        @Nullable String defaultPortoir,
        @Nullable Integer defaultConservationDays,
        UnitsResponse units,
        AnalyticalDelayResponse delay,
        List<ComponentLinkResponse> components,
        List<TechniqueLinkResponse> techniques,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public record DisciplineSnapshotResponse(String id, String code, String name) {}

    public record MatrixSnapshotResponse(String id, String code, String name) {}

    public record RecipientSnapshotResponse(String id, String code, String name) {}

    public record UnitsResponse(
            @Nullable String unit1,
            @Nullable Integer unit1Decimals,
            @Nullable String unit2,
            @Nullable Integer unit2Decimals,
            @Nullable BigDecimal conversionFactor) {}

    public record AnalyticalDelayResponse(
            @Nullable Integer days, @Nullable Integer hours, @Nullable Integer anteriorityCount) {}
}
