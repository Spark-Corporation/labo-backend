package org.sparkcorporation.lims.pdf.api;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * Full read-side carrier for a row in {@code pdf_render_jobs}. Returned by {@link
 * PdfRenderingService#findJobRecord}. The byte payload and the requesting user id are internal
 * fields used by the polling controller (for content-disposition responses and ownership checks
 * respectively); the JSON polling response strips them via {@link #toView()}.
 *
 * <p>The {@code bytes} field is populated only when {@link #status} is {@link
 * PdfRenderJobStatus#DONE}; {@code suggestedFilename} is null until the worker assigns one.
 */
public record PdfRenderJobRecord(
        String id,
        String documentTypeId,
        PdfRenderJobStatus status,
        byte @Nullable [] bytes,
        @Nullable String suggestedFilename,
        @Nullable String errorMessage,
        Instant requestedAt,
        @Nullable Instant completedAt,
        @Nullable String requestedByUserId) {

    /**
     * Project to the JSON response DTO. Drops {@code bytes} and {@code requestedByUserId} — neither
     * belongs in the polling response body.
     */
    public PdfRenderJobView toView() {
        return new PdfRenderJobView(
                id,
                documentTypeId,
                status,
                suggestedFilename,
                errorMessage,
                requestedAt,
                completedAt);
    }
}
