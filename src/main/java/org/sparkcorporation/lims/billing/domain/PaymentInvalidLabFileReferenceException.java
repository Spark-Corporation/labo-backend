package org.sparkcorporation.lims.billing.domain;

/**
 * Raised by {@code PaymentService.record} when the supplied {@code labFileId} does not resolve via
 * {@link org.sparkcorporation.lims.order.api.LabFileApi#existsById}. Mapped to {@code 404 Not
 * Found} via {@code BillingProblemDetailAdvice}.
 */
public class PaymentInvalidLabFileReferenceException extends RuntimeException {

    private final String labFileId;

    public PaymentInvalidLabFileReferenceException(String labFileId) {
        super("lab file not found: " + labFileId);
        this.labFileId = labFileId;
    }

    public String labFileId() {
        return labFileId;
    }
}
