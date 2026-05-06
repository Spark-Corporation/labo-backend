package org.sparkcorporation.lims.catalog.web;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.catalog.domain.Technique;

/** Hand-written mapper between {@link Technique} domain and web DTOs. */
@Component
public class TechniqueMapper {

    public TechniqueResponse toResponse(Technique t) {
        return new TechniqueResponse(
                t.id().value(),
                t.code(),
                t.name(),
                t.description(),
                t.deletedAt(),
                t.createdAt(),
                t.updatedAt(),
                t.version());
    }
}
