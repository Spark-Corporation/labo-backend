package org.sparkcorporation.lims.catalog.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.catalog.api.PartnerId;

public class PartnerNotFoundException extends EntityNotFoundException {

    public PartnerNotFoundException(PartnerId id) {
        super("Partner not found: " + id.value());
    }
}
