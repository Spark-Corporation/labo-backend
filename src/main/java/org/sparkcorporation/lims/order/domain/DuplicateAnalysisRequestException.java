package org.sparkcorporation.lims.order.domain;

/** Mapped → 422 by {@code OrderProblemDetailAdvice} (Chunk 5). */
public class DuplicateAnalysisRequestException extends RuntimeException {

    public DuplicateAnalysisRequestException(String analysisCode) {
        super("analysis already requested on this file: " + analysisCode);
    }
}
