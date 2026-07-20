package org.sparkcorporation.lims.catalog.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.catalog.api.InsuranceId;

public class InsuranceNotFoundException extends EntityNotFoundException {

    public InsuranceNotFoundException(InsuranceId id) {
        super("Insurance not found: " + id.value());
    }
}
