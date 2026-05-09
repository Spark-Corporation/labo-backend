package org.sparkcorporation.lims.order.domain;

/** Mapped → 422 by {@code OrderProblemDetailAdvice} (Chunk 5). */
public class AnalysisHasSamplesException extends RuntimeException {

    public AnalysisHasSamplesException(String analysisCode) {
        super("cannot remove analysis while samples reference it: " + analysisCode);
    }
}
