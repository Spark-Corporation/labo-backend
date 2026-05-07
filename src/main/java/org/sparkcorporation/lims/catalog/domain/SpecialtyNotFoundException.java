package org.sparkcorporation.lims.catalog.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.catalog.api.SpecialtyId;

public class SpecialtyNotFoundException extends EntityNotFoundException {

    public SpecialtyNotFoundException(SpecialtyId id) {
        super("Specialty not found: " + id.value());
    }
}
