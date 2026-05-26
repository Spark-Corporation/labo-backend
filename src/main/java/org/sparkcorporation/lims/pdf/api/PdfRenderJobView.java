package org.sparkcorporation.lims.pdf.api;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * Read-side JSON response DTO for a row in {@code pdf_render_jobs}. Returned by the polling
 * endpoint when {@code ?download=false} (the default). For the byte payload, the caller switches to
 * {@code ?download=true} which streams {@code application/pdf} directly. {@link
 * PdfRenderJobRecord#toView()} produces an instance from an internal record.
 */
public record PdfRenderJobView(
        String id,
        String documentTypeId,
        PdfRenderJobStatus status,
        @Nullable String suggestedFilename,
        @Nullable String errorMessage,
        Instant requestedAt,
        @Nullable Instant completedAt) {}
