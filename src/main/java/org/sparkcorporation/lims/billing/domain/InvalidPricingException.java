package org.sparkcorporation.lims.billing.domain;

public class InvalidPricingException extends RuntimeException {

    public InvalidPricingException(String message) {
        super(message);
    }
}
