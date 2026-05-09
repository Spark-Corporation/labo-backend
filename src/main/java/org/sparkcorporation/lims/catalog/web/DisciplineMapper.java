package org.sparkcorporation.lims.catalog.web;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.catalog.domain.Discipline;

/** Hand-written mapper between {@link Discipline} domain and web DTOs. */
@Component
public class DisciplineMapper {

    public DisciplineResponse toResponse(Discipline d) {
        return new DisciplineResponse(
                d.id().value(),
                d.code(),
                d.name(),
                d.description(),
                d.disciplinePrefix(),
                d.deletedAt(),
                d.createdAt(),
                d.updatedAt(),
                d.version());
    }
}
