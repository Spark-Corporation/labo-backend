package org.sparkcorporation.lims.catalog.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.catalog.api.AntibioticId;

public class AntibioticNotFoundException extends EntityNotFoundException {

    public AntibioticNotFoundException(AntibioticId id) {
        super("antibiotic not found: " + id.value());
    }
}
