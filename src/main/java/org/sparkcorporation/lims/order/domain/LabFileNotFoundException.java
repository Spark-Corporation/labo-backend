package org.sparkcorporation.lims.order.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.order.api.LabFileId;

/** Mapped → 404 by {@code shared.web.ProblemDetailAdvice} via {@link EntityNotFoundException}. */
public class LabFileNotFoundException extends EntityNotFoundException {

    public LabFileNotFoundException(LabFileId id) {
        super("lab file not found: " + id.value());
    }
}
