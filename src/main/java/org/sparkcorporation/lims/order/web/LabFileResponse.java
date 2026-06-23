package org.sparkcorporation.lims.order.web;

import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.shared.domain.Sex;

/**
 * Web-tier mirror of {@link org.sparkcorporation.lims.order.api.LabFileSummary}. Carries flat
 * snapshots of the linked patient / prescriber / préleveur, the sealed-style sample list, and the
 * analysis-request list.
 */
public record LabFileResponse(
        String id,
        String registrationNumber,
        PatientSnapshotResponse patient,
        @Nullable PrescriberSnapshotResponse prescriber,
        @Nullable PreleveurSnapshotResponse preleveur,
        boolean urgent,
        LabFileStatus status,
        List<SampleResponse> samples,
        List<AnalysisRequestResponse> analyses,
        Instant createdAt,
        @Nullable String createdByUserId,
        @Nullable Instant rendezVousAt) {

    public LabFileResponse {
        samples = List.copyOf(samples);
        analyses = List.copyOf(analyses);
    }

    /** Flat snapshot of the linked patient — never null. */
    public record PatientSnapshotResponse(
            String id, String labCode, String lastName, @Nullable String firstNames, Sex sex) {}

    /** Flat snapshot of the linked prescriber — nullable. */
    public record PrescriberSnapshotResponse(
            String id, String code, String fullName, @Nullable String specialtyCode) {}

    /** Flat snapshot of the linked Préleveur — nullable. */
    public record PreleveurSnapshotResponse(String id, String code, String fullName) {}
}
