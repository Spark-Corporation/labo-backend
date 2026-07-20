package org.sparkcorporation.lims.billing.web;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.billing.api.CashRegisterSummary;
import org.sparkcorporation.lims.billing.domain.NewCashRegister;
import org.sparkcorporation.lims.billing.domain.UpdateCashRegister;

@Component
public class CashRegisterMapper {

    public NewCashRegister toCommand(CashRegisterRequest.Create req) {
        return new NewCashRegister(req.code(), req.label(), req.sequenceNumber());
    }

    public UpdateCashRegister toCommand(CashRegisterRequest.Update req) {
        return new UpdateCashRegister(req.label());
    }

    public CashRegisterResponse toResponse(CashRegisterSummary s) {
        return new CashRegisterResponse(
                s.id().value(),
                s.code(),
                s.label(),
                s.sequenceNumber(),
                s.userIds(),
                s.disabled(),
                s.createdAt());
    }
}
