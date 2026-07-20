package org.sparkcorporation.lims.order.domain;

/**
 * Analysis reference (code) supplied did not resolve via {@code AnalysisApi.findByCode} — covers
 * both the "not found" and "found but inactive" cases via distinct messages.
 */
public final class OrderInvalidAnalysisReferenceException extends OrderInvalidReferenceException {

    public OrderInvalidAnalysisReferenceException(String analysisRef, String reason) {
        super("analysis", analysisRef, "analysis " + reason + ": " + analysisRef);
    }
}
