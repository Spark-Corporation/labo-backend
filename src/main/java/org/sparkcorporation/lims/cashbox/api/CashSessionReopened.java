package org.sparkcorporation.lims.cashbox.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.iam.api.UserId;

public record CashSessionReopened(
        CashRegisterId cashRegisterId,
        LocalDate sessionDate,
        String reason,
        UserId reopenedBy,
        Instant reopenedAt) {

    public CashSessionReopened {
        Objects.requireNonNull(cashRegisterId, "cashRegisterId");
        Objects.requireNonNull(sessionDate, "sessionDate");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(reopenedBy, "reopenedBy");
        Objects.requireNonNull(reopenedAt, "reopenedAt");
    }
}
