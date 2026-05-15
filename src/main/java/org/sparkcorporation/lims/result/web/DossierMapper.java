package org.sparkcorporation.lims.result.web;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.patient.api.PatientSummary;
import org.sparkcorporation.lims.result.infra.DossierRowProjection;
import org.sparkcorporation.lims.result.infra.OverdueDossierRowProjection;

/**
 * Static, stateless mapper from the native-query projection + hydrated cross-module summaries to
 * the API {@link DossierRow}. Marker string → enum mapping happens here.
 */
public final class DossierMapper {

    private DossierMapper() {
        throw new AssertionError("utility class");
    }

    public static DossierRow toRow(
            DossierRowProjection projection,
            @Nullable LabFileSummary labFile,
            @Nullable PatientSummary patient) {
        return new DossierRow(
                projection.getLabFileId(),
                labFile == null ? null : labFile.registrationNumber().value(),
                labFile == null ? null : labFile.createdAt(),
                labFile != null && labFile.urgent(),
                projection.getPatientId(),
                patient == null ? null : patient.labCode(),
                patient == null ? null : patient.lastName(),
                patient == null ? null : patient.firstNames(),
                patient != null && patient.insurance() != null,
                DossierMarker.valueOf(projection.getMarker()));
    }

    public static OverdueDossierRow toOverdueRow(
            OverdueDossierRowProjection projection,
            @Nullable LabFileSummary labFile,
            @Nullable PatientSummary patient) {
        DossierRowProjection adapter =
                new DossierRowProjection() {
                    @Override
                    public String getLabFileId() {
                        return projection.getLabFileId();
                    }

                    @Override
                    public String getPatientId() {
                        return projection.getPatientId();
                    }

                    @Override
                    public String getMarker() {
                        return projection.getMarker();
                    }
                };
        var inner = toRow(adapter, labFile, patient);
        var overdue =
                new OverdueDossierRow.OverdueInfo(
                        projection.getAnalysisCode(),
                        projection.getAnalysisName(),
                        projection.getDeadline(),
                        projection.getHoursPastDeadline(),
                        projection.getOverdueAnalysisCount());
        return new OverdueDossierRow(inner, overdue);
    }
}
