package org.sparkcorporation.lims.pdf.api;

/**
 * Thrown by {@link PdfRenderingService#findJobRecord} when the id has no row. Mapped to HTTP 404 by
 * {@code shared/web/ProblemDetailAdvice} (chunk 5).
 */
public final class PdfJobNotFoundException extends PdfRenderException {

    private final String jobId;

    public PdfJobNotFoundException(String jobId) {
        super(/* documentTypeId */ "<unknown>", /* reasonTag */ "job_not_found", null);
        this.jobId = jobId;
    }

    public String jobId() {
        return jobId;
    }
}
