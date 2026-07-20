package org.sparkcorporation.lims.cashbox.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.iam.api.UserId;

public record CashSessionClosed(
        CashRegisterId cashRegisterId,
        LocalDate sessionDate,
        BigDecimal grandTotal,
        UserId closedBy,
        Instant closedAt) {

    public CashSessionClosed {
        Objects.requireNonNull(cashRegisterId, "cashRegisterId");
        Objects.requireNonNull(sessionDate, "sessionDate");
        Objects.requireNonNull(grandTotal, "grandTotal");
        Objects.requireNonNull(closedBy, "closedBy");
        Objects.requireNonNull(closedAt, "closedAt");
    }
}
