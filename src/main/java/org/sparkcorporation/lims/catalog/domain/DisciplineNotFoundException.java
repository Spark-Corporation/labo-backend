package org.sparkcorporation.lims.catalog.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.catalog.api.DisciplineId;

public class DisciplineNotFoundException extends EntityNotFoundException {

    public DisciplineNotFoundException(DisciplineId id) {
        super("discipline not found: " + id.value());
    }
}
