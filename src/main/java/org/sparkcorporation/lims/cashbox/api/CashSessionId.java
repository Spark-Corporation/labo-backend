package org.sparkcorporation.lims.cashbox.api;

import java.time.LocalDate;
import java.util.Objects;

import org.sparkcorporation.lims.billing.api.CashRegisterId;

public record CashSessionId(CashRegisterId cashRegisterId, LocalDate sessionDate) {

    public CashSessionId {
        Objects.requireNonNull(cashRegisterId, "cashRegisterId");
        Objects.requireNonNull(sessionDate, "sessionDate");
    }
}
