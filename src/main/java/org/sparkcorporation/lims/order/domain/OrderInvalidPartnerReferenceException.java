package org.sparkcorporation.lims.order.domain;

/** Partner ID supplied at file open/update did not resolve via {@code PartnerApi.exists}. */
public final class OrderInvalidPartnerReferenceException extends OrderInvalidReferenceException {

    public OrderInvalidPartnerReferenceException(String partnerId) {
        super("partner", partnerId, "partner not found: " + partnerId);
    }
}
