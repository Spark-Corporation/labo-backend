package org.sparkcorporation.lims.order.web;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.shared.domain.Sex;

/**
 * Web-tier mirror of {@link org.sparkcorporation.lims.order.api.LabFileSummary}. Carries flat
 * snapshots of the linked patient / prescriber / préleveur, the sealed-style sample list, and the
 * analysis-request list.
 */
@Schema(
        description =
                "A lab file (dossier): the patient case that groups samples, requested analyses,"
                        + " and their lifecycle status.")
public record LabFileResponse(
        @Schema(description = "Unique identifier of the lab file.") String id,
        @Schema(
                        description =
                                "Human-readable registration number assigned when the dossier was"
                                        + " opened; immutable.")
                String registrationNumber,
        @Schema(description = "Flat snapshot of the patient this dossier belongs to.")
                PatientSnapshotResponse patient,
        @Schema(description = "Flat snapshot of the prescribing doctor, if any.")
                @Nullable PrescriberSnapshotResponse prescriber,
        @Schema(
                        description =
                                "Flat snapshot of the assigned Préleveur (home-collection agent),"
                                        + " if any.")
                @Nullable PreleveurSnapshotResponse preleveur,
        @Schema(description = "Whether this dossier is flagged urgent (rush processing).")
                boolean urgent,
        @Schema(
                        description =
                                "Current lifecycle status of the dossier (OPEN, AWAITING_RESULTS,"
                                        + " TECHNICALLY_VALIDATED, BIOLOGICALLY_VALIDATED, SIGNED,"
                                        + " PUBLISHED).")
                LabFileStatus status,
        @Schema(description = "Samples collected or received for this dossier.")
                List<SampleResponse> samples,
        @Schema(description = "Catalog analyses requested on this dossier.")
                List<AnalysisRequestResponse> analyses,
        @Schema(description = "Timestamp at which the dossier was opened.") Instant createdAt,
        @Schema(description = "Identifier of the staff user who opened the dossier, if known.")
                @Nullable String createdByUserId,
        @Schema(description = "Scheduled appointment (rendez-vous) time for this dossier, if any.")
                @Nullable Instant rendezVousAt) {

    public LabFileResponse {
        samples = List.copyOf(samples);
        analyses = List.copyOf(analyses);
    }

    /** Flat snapshot of the linked patient — never null. */
    @Schema(description = "Flat snapshot of a patient, as captured on the dossier.")
    public record PatientSnapshotResponse(
            @Schema(description = "Identifier of the patient.") String id,
            @Schema(description = "Lab-internal patient code.") String labCode,
            @Schema(description = "Patient's last name.") String lastName,
            @Schema(description = "Patient's first name(s), if recorded.")
                    @Nullable String firstNames,
            @Schema(description = "Patient's sex.") Sex sex) {}

    /** Flat snapshot of the linked prescriber — nullable. */
    @Schema(description = "Flat snapshot of a prescribing doctor, as captured on the dossier.")
    public record PrescriberSnapshotResponse(
            @Schema(description = "Identifier of the prescriber.") String id,
            @Schema(description = "Lab-internal prescriber code.") String code,
            @Schema(description = "Prescriber's full name.") String fullName,
            @Schema(description = "Prescriber's medical specialty code, if recorded.")
                    @Nullable String specialtyCode) {}

    /** Flat snapshot of the linked Préleveur — nullable. */
    @Schema(
            description =
                    "Flat snapshot of a Préleveur (home-collection agent), as captured on the"
                            + " dossier.")
    public record PreleveurSnapshotResponse(
            @Schema(description = "Identifier of the Préleveur.") String id,
            @Schema(description = "Lab-internal Préleveur code.") String code,
            @Schema(description = "Préleveur's full name.") String fullName) {}
}
