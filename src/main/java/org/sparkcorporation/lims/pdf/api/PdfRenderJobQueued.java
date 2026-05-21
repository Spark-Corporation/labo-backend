package org.sparkcorporation.lims.pdf.api;

import java.time.Instant;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Event published after a PDF render job has been persisted to {@code pdf_render_jobs} in status
 * {@code QUEUED}. The async worker listens for this event via {@code @TransactionalEventListener}
 * (phase {@code AFTER_COMMIT}) and flips the row to {@code RUNNING} before rendering.
 *
 * <p>{@code requestedByUserId} is {@code null} for system-issued renders that have no authenticated
 * principal.
 */
public record PdfRenderJobQueued(
        String jobId,
        String documentTypeId,
        @Nullable String requestedByUserId,
        Instant requestedAt) {

    public PdfRenderJobQueued {
        Objects.requireNonNull(jobId, "jobId");
        Objects.requireNonNull(documentTypeId, "documentTypeId");
        Objects.requireNonNull(requestedAt, "requestedAt");
    }
}
