package org.sparkcorporation.lims.catalog.web;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.catalog.api.InsuranceSummary;
import org.sparkcorporation.lims.catalog.domain.NewInsurance;
import org.sparkcorporation.lims.catalog.domain.UpdateInsurance;

@Component
public class InsuranceMapper {

    public NewInsurance toCommand(InsuranceRequest.Create req) {
        return new NewInsurance(
                req.code(),
                req.name(),
                req.responsibleEmail(),
                req.responsibleContact(),
                req.bRate(),
                req.coverageRate());
    }

    public UpdateInsurance toCommand(InsuranceRequest.Update req) {
        return new UpdateInsurance(
                req.name(),
                req.responsibleEmail(),
                req.responsibleContact(),
                req.bRate(),
                req.coverageRate());
    }

    public InsuranceResponse toResponse(InsuranceSummary s) {
        return new InsuranceResponse(
                s.id().value(),
                s.code(),
                s.name(),
                s.responsibleEmail(),
                s.responsibleContact(),
                s.bRate(),
                s.coverageRate(),
                s.disabled());
    }
}
