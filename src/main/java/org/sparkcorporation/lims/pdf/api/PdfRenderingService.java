package org.sparkcorporation.lims.pdf.api;

import java.util.Optional;

/**
 * Public engine API for HTML → PDF rendering. The implementation lives in {@code
 * pdf.infra.OpenHtmlToPdfRenderingService} (chunk 3 sync; chunk 5 async).
 */
public interface PdfRenderingService {

    /**
     * Synchronous render. Blocks the caller thread.
     *
     * @throws PdfRenderException on any engine failure (template, model, IO).
     */
    <T> RenderedPdf render(PdfDocumentType<T> docType, T model);

    /**
     * Enqueue an async render. Returns the new job id immediately. Bytes appear on the row when the
     * worker completes.
     *
     * @throws PdfRenderException with reason {@code "executor_saturated"} when the executor queue
     *     is full (mapped to HTTP 503 by the controller).
     */
    <T> String enqueue(PdfDocumentType<T> docType, T model);

    /** Look up a job by id. {@link Optional#empty()} for unknown id. */
    Optional<PdfRenderJobView> findJob(String jobId);

    /**
     * Look up the original requester userId for a job. {@link Optional#empty()} for either an
     * unknown id OR a system-issued render (no authenticated principal at enqueue time).
     */
    Optional<String> findJobOwner(String jobId);
}
