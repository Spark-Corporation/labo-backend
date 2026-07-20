package org.sparkcorporation.lims.catalog.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.catalog.api.MatrixId;

public class MatrixNotFoundException extends EntityNotFoundException {

    public MatrixNotFoundException(MatrixId id) {
        super("matrix not found: " + id.value());
    }
}
