package org.sparkcorporation.lims.order.domain;

/** Préleveur ID supplied did not resolve via {@code PreleveurApi.existsById}. */
public final class OrderInvalidPreleveurReferenceException extends OrderInvalidReferenceException {

    public OrderInvalidPreleveurReferenceException(String preleveurId) {
        super("preleveur", preleveurId, "preleveur not found: " + preleveurId);
    }
}
