package org.sparkcorporation.lims.result.infra;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/** Flat row for the CDC §VII per-paillasse worksheet: one row per result line. */
public interface PaillasseWorksheetRowProjection {
    String getLabFileId();

    String getRegistrationNumber();

    String getPatientId();

    String getAnalysisCode();

    String getComponentCode();

    String getResultState();

    String getResultType();

    @Nullable Long getValueInteger();

    @Nullable BigDecimal getValueReal();

    @Nullable String getValueText();

    @Nullable String getUnit1Snapshot();

    boolean getIsOutOfRange();

    @Nullable String getReferenceIntervalId();

    @Nullable String getCommentText();

    String getAnalysisId();

    int getPosition();
}
