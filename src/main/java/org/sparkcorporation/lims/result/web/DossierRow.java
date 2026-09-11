package org.sparkcorporation.lims.result.web;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * Row shape for the §X dossier queue (urgent) and §IX state-filtered dossier list. Composes LabFile
 * + Patient projections with a computed CDC §X marker (vert / rouge / stylo).
 *
 * <p>Per spec §5.4: raw 4-state ResultState is intentionally NOT exposed on this row — consumers
 * needing it call the existing {@code GET /api/v1/results?labFileId=...} from 1.5.a. The
 * single-vocabulary discipline (CDC §X markers only) keeps the dossier-list API readable.
 *
 * <p>{@code labFileId} and {@code patientId} are raw ULID strings in the JSON response (not nested
 * typed-wrapper objects) to match the flat-string convention used across other response records.
 */
@Schema(description = "One row of a dossier (lab-file work) queue.")
public record DossierRow(
        @Schema(description = "Id of the lab file.") String labFileId,
        @Nullable @Schema(description = "Lab file's registration number.")
                String registrationNumber,
        @Nullable @Schema(description = "When the lab file was created.") Instant createdAt,
        @Schema(description = "Whether the lab file is flagged urgent.") boolean urgent,
        @Schema(description = "Id of the patient the lab file belongs to.") String patientId,
        @Nullable @Schema(description = "Patient's lab-internal code, if known.")
                String patientLabCode,
        @Nullable @Schema(description = "Patient's last name, if known.") String patientLastName,
        @Nullable @Schema(description = "Patient's first name(s), if known.")
                String patientFirstNames,
        @Schema(description = "Whether the patient is covered by insurance.")
                boolean patientInsured,
        @Schema(
                        description =
                                "Visual completion marker: GREEN (fully entered, no out-of-range"
                                        + " result), RED (fully entered, at least one"
                                        + " out-of-range result), or PEN (still partially"
                                        + " filled in).")
                DossierMarker marker,
        @Nullable @Schema(description = "When the dossier was last printed, if ever.")
                Instant printedAt,
        @Nullable @Schema(description = "When the dossier was last emailed, if ever.")
                Instant emailedAt) {}
