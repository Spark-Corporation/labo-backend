package org.sparkcorporation.lims.order.web;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * PUT {@code /api/v1/lab-files/{id}/rendezvous} payload. A {@code null} {@code rendezVousAt} clears
 * the appointment (this is an explicit set/clear, not the PATCH "no-change" sentinel).
 */
@Schema(description = "Request to schedule or clear the appointment (rendez-vous) on a lab file.")
public record RendezVousRequest(
        @Schema(
                        description =
                                "New scheduled appointment time; send null to explicitly clear the"
                                        + " appointment.")
                @Nullable Instant rendezVousAt) {}
