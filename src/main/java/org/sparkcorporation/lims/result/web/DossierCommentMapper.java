package org.sparkcorporation.lims.result.web;

import org.sparkcorporation.lims.result.api.DossierCommentSummary;

/**
 * Static-only mapper translating {@link DossierCommentSummary} → {@link DossierCommentResponse}.
 */
public final class DossierCommentMapper {

    private DossierCommentMapper() {}

    public static DossierCommentResponse toResponse(DossierCommentSummary s) {
        return new DossierCommentResponse(
                s.labFileId().value(), s.commentText(), s.createdAt(), s.updatedAt());
    }
}
