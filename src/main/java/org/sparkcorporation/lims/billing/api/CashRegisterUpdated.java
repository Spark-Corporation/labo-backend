package org.sparkcorporation.lims.billing.api;

import java.time.Instant;
import java.util.Objects;

/** Domain event — published after a {@code CashRegister}'s label or assignments are mutated. */
public record CashRegisterUpdated(CashRegisterId id, Instant occurredAt) {

    public CashRegisterUpdated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
