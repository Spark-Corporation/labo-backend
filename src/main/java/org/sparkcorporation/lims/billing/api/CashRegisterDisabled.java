package org.sparkcorporation.lims.billing.api;

import java.time.Instant;
import java.util.Objects;

/** Domain event — published after a {@code CashRegister} is soft-disabled. */
public record CashRegisterDisabled(CashRegisterId id, Instant occurredAt) {

    public CashRegisterDisabled {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
