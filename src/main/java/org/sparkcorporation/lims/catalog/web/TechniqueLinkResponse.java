package org.sparkcorporation.lims.catalog.web;

import io.swagger.v3.oas.annotations.media.Schema;

/** Per-row technique link with default flag. */
@Schema(description = "An analytical technique linked to an analysis.")
public record TechniqueLinkResponse(
        @Schema(description = "ULID of the technique.") String techniqueId,
        @Schema(description = "Code of the technique.") String code,
        @Schema(description = "Name of the technique.") String name,
        @Schema(
                        description =
                                "Whether this is the default technique for the analysis; at most"
                                        + " one link is default.")
                boolean isDefault) {}
