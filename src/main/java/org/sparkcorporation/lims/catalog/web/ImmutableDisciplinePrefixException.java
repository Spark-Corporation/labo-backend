package org.sparkcorporation.lims.catalog.web;

/**
 * Thrown when a PATCH body includes the immutable {@code disciplinePrefix} field on a Discipline.
 * Mirrors {@link ImmutableCodeException}. Mapped to 422 by {@link CatalogProblemDetailAdvice}.
 */
public class ImmutableDisciplinePrefixException extends RuntimeException {

    public ImmutableDisciplinePrefixException(String message) {
        super(message);
    }
}
