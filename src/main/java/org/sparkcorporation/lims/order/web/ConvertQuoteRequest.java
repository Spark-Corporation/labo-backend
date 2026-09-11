package org.sparkcorporation.lims.order.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * POST {@code /api/v1/quotes/{id}:convert} payload — passes the {@code urgent} flag through to the
 * implicit {@code LabFileService.create} call (spec §5.5).
 */
@Schema(description = "Request to convert a LOCKED quote into a new lab file (dossier).")
public record ConvertQuoteRequest(
        @Schema(
                        description =
                                "Whether the resulting lab file should be flagged urgent (rush"
                                        + " processing).")
                boolean urgent) {}
