package org.sparkcorporation.lims.result.web;

import java.math.BigDecimal;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.ReferenceIntervalId;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.result.api.ResultLineId;
import org.sparkcorporation.lims.result.api.ResultState;

public record ResultSearchRow(
        ResultId resultId,
        ResultLineId lineId,
        LabFileId labFileId,
        String labFileRegistrationNumber,
        PatientId patientId,
        @Nullable String patientLabCode,
        @Nullable String patientLastName,
        @Nullable String patientFirstNames,
        boolean patientInsured,
        String analysisCode,
        String componentCode,
        int position,
        ResultType resultType,
        @Nullable Long valueInteger,
        @Nullable BigDecimal valueReal,
        @Nullable String valueText,
        @Nullable String unit1Snapshot,
        boolean isOutOfRange,
        @Nullable ReferenceIntervalId referenceIntervalId,
        ResultState resultState,
        @Nullable Instant biologicalValidatedAt,
        Instant createdAt) {}
