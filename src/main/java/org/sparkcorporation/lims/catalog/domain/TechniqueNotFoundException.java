package org.sparkcorporation.lims.catalog.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.catalog.api.TechniqueId;

public class TechniqueNotFoundException extends EntityNotFoundException {

    public TechniqueNotFoundException(TechniqueId id) {
        super("technique not found: " + id.value());
    }
}
