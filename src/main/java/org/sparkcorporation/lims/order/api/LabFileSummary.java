package org.sparkcorporation.lims.order.api;

import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.shared.domain.Sex;

/**
 * Cross-module read snapshot of a LabFile aggregate.
 *
 * <p>Carries flat snapshots of the linked Patient / Prescriber / Préleveur (avoids cross-module
 * N+1) plus a sealed {@link SampleSummary} hierarchy and the analyses-on-file list. Snapshot
 * resolution policy: see spec §6.2 — soft-deleted children are surfaced (the dossier outlives
 * dictionary edits).
 */
public record LabFileSummary(
        LabFileId id,
        LabFileRegistrationNumber registrationNumber,
        PatientSnapshot patient,
        @Nullable PrescriberSnapshot prescriber,
        @Nullable PreleveurSnapshot preleveur,
        boolean urgent,
        LabFileStatus status,
        List<SampleSummary> samples,
        List<AnalysisRequestSummary> analyses,
        Instant createdAt,
        @Nullable String createdByUserId) {

    public LabFileSummary {
        samples = List.copyOf(samples);
        analyses = List.copyOf(analyses);
    }

    /** Flat snapshot of the linked patient at read time — never null (FK NOT NULL). */
    public record PatientSnapshot(
            String id, String labCode, String lastName, @Nullable String firstNames, Sex sex) {}

    /** Flat snapshot of the linked prescriber at read time — nullable on the LabFile. */
    public record PrescriberSnapshot(
            String id, String code, String fullName, @Nullable String specialtyCode) {}

    /** Flat snapshot of the linked Préleveur at read time — nullable on the LabFile. */
    public record PreleveurSnapshot(String id, String code, String fullName) {}

    /**
     * Sealed hierarchy mirrors the {@code Sample} aggregate's three sub-types — InLab / Home /
     * ExternalReceived. The discriminator value is implicit in the concrete record class.
     */
    public sealed interface SampleSummary
            permits InLabSampleSummary, HomeSampleSummary, ExternalReceivedSampleSummary {

        SampleId id();

        SampleDailyId dailyId();

        String analysisCode();

        String recipientCode();

        @Nullable Instant collectedAt();

        @Nullable String collectedByUserId();
    }

    public record InLabSampleSummary(
            SampleId id,
            SampleDailyId dailyId,
            String analysisCode,
            String recipientCode,
            Instant collectedAt,
            @Nullable String collectedByUserId)
            implements SampleSummary {}

    public record HomeSampleSummary(
            SampleId id,
            SampleDailyId dailyId,
            String analysisCode,
            String recipientCode,
            Instant collectedAt,
            @Nullable String collectedByUserId,
            String addressLine,
            int distanceKm,
            Instant windowStart,
            Instant windowEnd,
            String preleveurCode)
            implements SampleSummary {}

    public record ExternalReceivedSampleSummary(
            SampleId id,
            SampleDailyId dailyId,
            String analysisCode,
            String recipientCode,
            @Nullable Instant collectedAt,
            @Nullable String collectedByUserId,
            String origin,
            Instant receivedAt,
            String receivedByUserId)
            implements SampleSummary {}

    /** One requested analysis on a LabFile (denormalized {@code analysisCode} snapshot). */
    public record AnalysisRequestSummary(
            AnalysisRequestId id, String analysisId, String analysisCode, Instant requestedAt) {}
}
