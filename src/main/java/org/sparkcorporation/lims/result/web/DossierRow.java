package org.sparkcorporation.lims.result.web;

import java.time.Instant;

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
public record DossierRow(
        String labFileId,
        @Nullable String registrationNumber,
        @Nullable Instant createdAt,
        boolean urgent,
        String patientId,
        @Nullable String patientLabCode,
        @Nullable String patientLastName,
        @Nullable String patientFirstNames,
        boolean patientInsured,
        DossierMarker marker,
        @Nullable Instant printedAt,
        @Nullable Instant emailedAt) {}
