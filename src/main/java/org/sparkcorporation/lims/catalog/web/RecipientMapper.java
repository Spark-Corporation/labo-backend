package org.sparkcorporation.lims.catalog.web;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.catalog.domain.Recipient;

/** Hand-written mapper between {@link Recipient} domain and web DTOs. */
@Component
public class RecipientMapper {

    public RecipientResponse toResponse(Recipient r) {
        var matrix =
                r.defaultMatrix() == null
                        ? null
                        : new RecipientResponse.MatrixSnapshot(
                                r.defaultMatrix().id().value(),
                                r.defaultMatrix().code(),
                                r.defaultMatrix().name());
        return new RecipientResponse(
                r.id().value(),
                r.code(),
                r.name(),
                r.description(),
                r.color(),
                r.volumeMl(),
                matrix,
                r.deletedAt(),
                r.createdAt(),
                r.updatedAt(),
                r.version());
    }
}
