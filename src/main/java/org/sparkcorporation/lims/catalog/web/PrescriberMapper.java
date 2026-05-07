package org.sparkcorporation.lims.catalog.web;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.catalog.api.SpecialtyId;
import org.sparkcorporation.lims.catalog.domain.NewPrescriber;
import org.sparkcorporation.lims.catalog.domain.Prescriber;
import org.sparkcorporation.lims.catalog.domain.UpdatePrescriber;

/** Hand-written mapper between {@link Prescriber} domain and web DTOs. */
@Component
public class PrescriberMapper {

    public NewPrescriber toCommand(CreatePrescriberRequest req) {
        return new NewPrescriber(
                req.title(),
                req.name(),
                req.specialtyId() == null ? null : SpecialtyId.of(req.specialtyId()),
                req.phone(),
                req.email(),
                req.licenseNumber(),
                req.institution());
    }

    public UpdatePrescriber toCommand(UpdatePrescriberRequest req) {
        return new UpdatePrescriber(
                req.title(),
                req.name(),
                req.specialtyId() == null ? null : SpecialtyId.of(req.specialtyId()),
                req.phone(),
                req.email(),
                req.licenseNumber(),
                req.institution());
    }

    public PrescriberResponse toResponse(Prescriber p) {
        var snap =
                p.specialty() == null
                        ? null
                        : new PrescriberResponse.SpecialtySnapshotResponse(
                                p.specialty().rawId(), p.specialty().code(), p.specialty().name());
        return new PrescriberResponse(
                p.rawId(),
                p.code(),
                p.title(),
                p.name(),
                snap,
                p.phone(),
                p.email(),
                p.licenseNumber(),
                p.institution(),
                p.createdAt(),
                p.updatedAt(),
                p.version());
    }
}
