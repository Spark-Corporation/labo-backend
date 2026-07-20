package org.sparkcorporation.lims.result.web;

import java.math.BigDecimal;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.result.api.ResultLineId;

/**
 * Row shape for the §IV anterior-results lookup ({@code GET /api/v1/results/anterior}). Not
 * paginated — returned as a bounded {@code List} directly.
 */
public record AnteriorResultRow(
        ResultId resultId,
        ResultLineId lineId,
        LabFileId labFileId,
        String labFileRegistrationNumber,
        String componentCode,
        ResultType resultType,
        @Nullable Long valueInteger,
        @Nullable BigDecimal valueReal,
        @Nullable String valueText,
        @Nullable String unit1Snapshot,
        boolean isOutOfRange,
        Instant biologicalValidatedAt) {}
