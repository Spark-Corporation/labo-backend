package org.sparkcorporation.lims.result.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * PUT body for {@code PUT /api/v1/lab-files/{labFileId}/dossier-comment}. Per spec §4.4 the column
 * width is 4000.
 */
@Schema(description = "Body for creating or replacing a dossier's general comment")
public record UpsertDossierCommentRequest(
        @Schema(description = "Comment text (max 4000 characters)") @NotBlank @Size(max = 4000)
                String commentText) {}
