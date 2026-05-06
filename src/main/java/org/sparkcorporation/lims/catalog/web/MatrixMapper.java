package org.sparkcorporation.lims.catalog.web;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.catalog.domain.Matrix;

/** Hand-written mapper between {@link Matrix} domain and web DTOs. */
@Component
public class MatrixMapper {

    public MatrixResponse toResponse(Matrix m) {
        return new MatrixResponse(
                m.id().value(),
                m.code(),
                m.name(),
                m.description(),
                m.deletedAt(),
                m.createdAt(),
                m.updatedAt(),
                m.version());
    }
}
