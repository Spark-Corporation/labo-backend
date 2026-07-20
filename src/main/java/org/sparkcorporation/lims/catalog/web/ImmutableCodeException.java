package org.sparkcorporation.lims.catalog.web;

/**
 * Thrown when a PATCH body includes an immutable {@code code} field on a Specialty (spec §5.5).
 * Mapped to 422 by {@link CatalogProblemDetailAdvice}.
 */
public class ImmutableCodeException extends RuntimeException {

    public ImmutableCodeException(String message) {
        super(message);
    }
}
