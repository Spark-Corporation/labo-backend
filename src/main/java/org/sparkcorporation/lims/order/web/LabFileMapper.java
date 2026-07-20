package org.sparkcorporation.lims.order.web;

import org.sparkcorporation.lims.order.api.LabFileSummary;

/**
 * Hand-written mapper translating order summaries → web responses. Static-method style mirrors the
 * catalog precedent ({@code DisciplineMapper}) but with no instance state needed.
 */
public final class LabFileMapper {

    private LabFileMapper() {}

    public static LabFileResponse toResponse(LabFileSummary s) {
        var samples = s.samples().stream().map(SampleMapper::toResponse).toList();
        var analyses = s.analyses().stream().map(AnalysisRequestMapper::toResponse).toList();
        return new LabFileResponse(
                s.id().value(),
                s.registrationNumber().value(),
                toPatientSnapshot(s.patient()),
                s.prescriber() == null ? null : toPrescriberSnapshot(s.prescriber()),
                s.preleveur() == null ? null : toPreleveurSnapshot(s.preleveur()),
                s.urgent(),
                s.status(),
                samples,
                analyses,
                s.createdAt(),
                s.createdByUserId(),
                s.rendezVousAt());
    }

    private static LabFileResponse.PatientSnapshotResponse toPatientSnapshot(
            LabFileSummary.PatientSnapshot p) {
        return new LabFileResponse.PatientSnapshotResponse(
                p.id(), p.labCode(), p.lastName(), p.firstNames(), p.sex());
    }

    private static LabFileResponse.PrescriberSnapshotResponse toPrescriberSnapshot(
            LabFileSummary.PrescriberSnapshot p) {
        return new LabFileResponse.PrescriberSnapshotResponse(
                p.id(), p.code(), p.fullName(), p.specialtyCode());
    }

    private static LabFileResponse.PreleveurSnapshotResponse toPreleveurSnapshot(
            LabFileSummary.PreleveurSnapshot p) {
        return new LabFileResponse.PreleveurSnapshotResponse(p.id(), p.code(), p.fullName());
    }
}
