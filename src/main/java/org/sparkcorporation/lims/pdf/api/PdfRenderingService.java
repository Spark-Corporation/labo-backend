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

    /**
     * Look up a job with its full record. {@link Optional#empty()} for unknown id.
     *
     * <p>Replaces the now-removed {@code findJob} / {@code findJobOwner} pair: the record bundles
     * the view-able fields, the bytes, and the requester id so the polling controller can perform
     * its ownership check + status response in a single DB roundtrip.
     */
    Optional<PdfRenderJobRecord> findJobRecord(String jobId);
}
