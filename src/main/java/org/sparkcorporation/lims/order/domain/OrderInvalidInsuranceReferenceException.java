package org.sparkcorporation.lims.order.domain;

/** Insurance ID supplied at file open/update did not resolve via {@code InsuranceApi.exists}. */
public final class OrderInvalidInsuranceReferenceException extends OrderInvalidReferenceException {

    public OrderInvalidInsuranceReferenceException(String insuranceId) {
        super("insurance", insuranceId, "insurance not found: " + insuranceId);
    }
}
