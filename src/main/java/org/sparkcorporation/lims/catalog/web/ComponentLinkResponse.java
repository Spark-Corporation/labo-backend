package org.sparkcorporation.lims.catalog.web;

import io.swagger.v3.oas.annotations.media.Schema;

/** Per-row component link from a GROUPED/SOCIETY_PANEL parent to a SIMPLE child. */
@Schema(
        description =
                "A child SIMPLE analysis attached to a GROUPED/SOCIETY_PANEL parent analysis.")
public record ComponentLinkResponse(
        @Schema(description = "ULID of the child analysis.") String childId,
        @Schema(description = "Code of the child analysis.") String childCode,
        @Schema(description = "Name of the child analysis.") String childName,
        @Schema(
                        description =
                                "1-based display position of this component within the parent's"
                                        + " component list.")
                int position) {}
