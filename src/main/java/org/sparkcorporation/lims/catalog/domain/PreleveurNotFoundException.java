package org.sparkcorporation.lims.catalog.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.catalog.api.PreleveurId;

public class PreleveurNotFoundException extends EntityNotFoundException {

    public PreleveurNotFoundException(PreleveurId id) {
        super("Preleveur not found: " + id.value());
    }
}
