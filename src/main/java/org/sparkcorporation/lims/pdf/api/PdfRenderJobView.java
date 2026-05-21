package org.sparkcorporation.lims.pdf.api;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * Read-side view of a row in {@code pdf_render_jobs}. Returned by {@link
 * PdfRenderingService#findJob}. Byte payload is populated only when {@link #status} is {@link
 * PdfRenderJobStatus#DONE}.
 */
public record PdfRenderJobView(
        String id,
        String documentTypeId,
        PdfRenderJobStatus status,
        @Nullable byte[] bytes,
        @Nullable String suggestedFilename,
        @Nullable String errorMessage,
        Instant requestedAt,
        @Nullable Instant completedAt) {}
