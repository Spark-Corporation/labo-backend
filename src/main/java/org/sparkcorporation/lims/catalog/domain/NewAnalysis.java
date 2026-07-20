package org.sparkcorporation.lims.catalog.domain;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.DisciplineId;
import org.sparkcorporation.lims.catalog.api.MatrixId;
import org.sparkcorporation.lims.catalog.api.RecipientId;
import org.sparkcorporation.lims.catalog.api.ResultType;

/**
 * Command to create an {@code Analysis}. {@code code} is admin-typed (validated by {@code
 * AnalysisCode.of(...)} on entry into the aggregate). Required fields are non-null at type level;
 * optional fields are marked {@link Nullable}.
 */
public record NewAnalysis(
        String code,
        String name,
        AnalysisKind kind,
        BigDecimal indiciaryValueB,
        int labelCount,
        DisciplineId disciplineId,
        @Nullable MatrixId matrixId,
        @Nullable RecipientId recipientId,
        ResultType resultType,
        @Nullable String interpretationText,
        @Nullable String formulaExpression,
        @Nullable String defaultPortoir,
        @Nullable Integer defaultConservationDays,
        @Nullable Units units,
        @Nullable AnalyticalDelay delay) {}
