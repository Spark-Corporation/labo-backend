package org.sparkcorporation.lims.catalog.web;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.catalog.domain.Antibiotic;

/** Hand-written mapper between {@link Antibiotic} domain and web DTOs. */
@Component
public class AntibioticMapper {

    public AntibioticResponse toResponse(Antibiotic a) {
        return new AntibioticResponse(
                a.id().value(),
                a.code(),
                a.name(),
                a.description(),
                a.family(),
                a.referenceDiameter(),
                a.intermediateLower(),
                a.intermediateUpper(),
                a.deletedAt(),
                a.createdAt(),
                a.updatedAt(),
                a.version());
    }
}
