package org.sparkcorporation.lims.order.domain;

/** Prescriber ID supplied did not resolve via {@code PrescriberApi.existsById}. */
public final class OrderInvalidPrescriberReferenceException extends OrderInvalidReferenceException {

    public OrderInvalidPrescriberReferenceException(String prescriberId) {
        super("prescriber", prescriberId, "prescriber not found: " + prescriberId);
    }
}
