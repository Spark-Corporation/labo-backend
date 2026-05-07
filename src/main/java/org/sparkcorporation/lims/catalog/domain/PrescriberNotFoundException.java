package org.sparkcorporation.lims.catalog.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.catalog.api.PrescriberId;

public class PrescriberNotFoundException extends EntityNotFoundException {

    public PrescriberNotFoundException(PrescriberId id) {
        super("Prescriber not found: " + id.value());
    }
}
