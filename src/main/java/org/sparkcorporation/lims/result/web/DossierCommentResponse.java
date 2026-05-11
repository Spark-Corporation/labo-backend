package org.sparkcorporation.lims.result.web;

import java.time.Instant;

/** Web-tier mirror of {@link org.sparkcorporation.lims.result.api.DossierCommentSummary}. */
public record DossierCommentResponse(
        String labFileId, String commentText, Instant createdAt, Instant updatedAt) {}
