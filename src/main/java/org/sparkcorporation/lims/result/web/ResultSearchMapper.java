package org.sparkcorporation.lims.result.web;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.ReferenceIntervalId;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientSummary;
import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.result.api.ResultLineId;
import org.sparkcorporation.lims.result.api.ResultState;
import org.sparkcorporation.lims.result.infra.ResultSearchRowProjection;

public final class ResultSearchMapper {

    private ResultSearchMapper() {
        throw new AssertionError("utility class");
    }

    public static ResultSearchRow toRow(
            ResultSearchRowProjection projection, @Nullable PatientSummary patient) {
        return new ResultSearchRow(
                new ResultId(projection.getResultId()),
                new ResultLineId(projection.getLineId()),
                new LabFileId(projection.getLabFileId()),
                projection.getLabFileRegistrationNumber(),
                new PatientId(projection.getPatientId()),
                patient == null ? null : patient.labCode(),
                patient == null ? null : patient.lastName(),
                patient == null ? null : patient.firstNames(),
                patient != null && patient.insured(),
                projection.getAnalysisCode(),
                projection.getComponentCode(),
                projection.getPosition(),
                ResultType.valueOf(projection.getResultType()),
                projection.getValueInteger(),
                projection.getValueReal(),
                projection.getValueText(),
                projection.getUnit1Snapshot(),
                projection.isOutOfRange(),
                projection.getReferenceIntervalId() == null
                        ? null
                        : new ReferenceIntervalId(projection.getReferenceIntervalId()),
                ResultState.valueOf(projection.getResultState()),
                projection.getBiologicalValidatedAt(),
                projection.getCreatedAt());
    }
}
