package org.sparkcorporation.lims.result.web;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/** Web-tier mirror of {@link org.sparkcorporation.lims.result.api.DossierCommentSummary}. */
@Schema(description = "The dossier's single general-comment block")
public record DossierCommentResponse(
        @Schema(description = "ULID of the lab file this comment is attached to") String labFileId,
        @Schema(description = "Free-text comment content (max 4000 characters)") String commentText,
        @Schema(description = "Timestamp the comment was first created") Instant createdAt,
        @Schema(description = "Timestamp the comment was last updated") Instant updatedAt) {}
