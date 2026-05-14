package org.sparkcorporation.lims.catalog.web;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.catalog.api.PartnerSummary;
import org.sparkcorporation.lims.catalog.domain.NewPartner;
import org.sparkcorporation.lims.catalog.domain.UpdatePartner;

@Component
public class PartnerMapper {

    public NewPartner toCommand(PartnerRequest.Create req) {
        return new NewPartner(
                req.code(),
                req.name(),
                req.kind(),
                req.responsibleEmail(),
                req.responsibleContact(),
                req.bRate());
    }

    public UpdatePartner toCommand(PartnerRequest.Update req) {
        return new UpdatePartner(
                req.name(),
                req.kind(),
                req.responsibleEmail(),
                req.responsibleContact(),
                req.bRate());
    }

    public PartnerResponse toResponse(PartnerSummary s) {
        return new PartnerResponse(
                s.id().value(),
                s.code(),
                s.name(),
                s.kind(),
                s.responsibleEmail(),
                s.responsibleContact(),
                s.bRate(),
                s.disabled());
    }
}
