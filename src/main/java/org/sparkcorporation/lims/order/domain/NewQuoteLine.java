package org.sparkcorporation.lims.order.domain;

import java.math.BigDecimal;

/**
 * Command for adding a single line to a DRAFT {@link Quote}. The catalog snapshots ({@code
 * indiciaryValueBSnapshot} + {@code labelCountSnapshot}) are captured at the cross-module boundary
 * (post-resolution via {@code AnalysisApi.findByCode}) so the line is immutable to dictionary edits
 * after creation.
 */
public record NewQuoteLine(
        String analysisId,
        String analysisCode,
        BigDecimal indiciaryValueBSnapshot,
        int labelCountSnapshot) {}
