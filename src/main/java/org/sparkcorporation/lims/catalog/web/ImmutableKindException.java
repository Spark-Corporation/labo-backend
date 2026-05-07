package org.sparkcorporation.lims.catalog.web;

/**
 * Thrown when a PATCH body includes the immutable {@code kind} field on an Analysis (spec §5.5).
 * Use the {@code :convert-to-grouped} / {@code :convert-to-simple} endpoints to change kind. Mapped
 * to 422 by {@link CatalogProblemDetailAdvice}.
 */
public class ImmutableKindException extends RuntimeException {

    public ImmutableKindException(String message) {
        super(message);
    }
}
