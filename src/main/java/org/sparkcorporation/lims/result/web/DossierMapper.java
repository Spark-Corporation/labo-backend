package org.sparkcorporation.lims.result.web;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.patient.api.PatientSummary;
import org.sparkcorporation.lims.result.infra.DossierRowProjection;

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
                patient != null && patient.insured(),
                DossierMarker.valueOf(projection.getMarker()));
    }
}
