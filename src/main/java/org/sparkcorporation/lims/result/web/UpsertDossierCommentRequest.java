package org.sparkcorporation.lims.result.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PUT body for {@code PUT /api/v1/lab-files/{labFileId}/dossier-comment}. Per spec §4.4 the column
 * width is 4000.
 */
public record UpsertDossierCommentRequest(@NotBlank @Size(max = 4000) String commentText) {}
