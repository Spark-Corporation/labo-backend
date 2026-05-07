package org.sparkcorporation.lims.catalog.web;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.catalog.domain.NewSpecialty;
import org.sparkcorporation.lims.catalog.domain.Specialty;
import org.sparkcorporation.lims.catalog.domain.UpdateSpecialty;

/** Hand-written mapper between {@link Specialty} domain and web DTOs. */
@Component
public class SpecialtyMapper {

    public NewSpecialty toCommand(CreateSpecialtyRequest req) {
        return new NewSpecialty(req.code(), req.name(), req.description());
    }

    public UpdateSpecialty toCommand(UpdateSpecialtyRequest req) {
        return new UpdateSpecialty(req.name(), req.description());
    }

    public SpecialtyResponse toResponse(Specialty s) {
        return new SpecialtyResponse(
                s.rawId(),
                s.code(),
                s.name(),
                s.description(),
                s.createdAt(),
                s.updatedAt(),
                s.version());
    }
}
