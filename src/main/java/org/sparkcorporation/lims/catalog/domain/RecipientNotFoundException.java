package org.sparkcorporation.lims.catalog.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.catalog.api.RecipientId;

public class RecipientNotFoundException extends EntityNotFoundException {

    public RecipientNotFoundException(RecipientId id) {
        super("recipient not found: " + id.value());
    }
}
