package org.sparkcorporation.lims.catalog.domain;

/** Mapped → 409 by CatalogProblemDetailAdvice (Chunk 5). */
public class CodeAlreadyExistsException extends RuntimeException {

    public CodeAlreadyExistsException(String entityName, String code) {
        super(entityName + " code already exists: " + code);
    }
}
