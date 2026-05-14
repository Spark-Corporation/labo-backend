package org.sparkcorporation.lims.billing.api;

import java.time.Instant;
import java.util.Objects;

/** Domain event — published after a new {@code CashRegister} is persisted. */
public record CashRegisterCreated(CashRegisterId id, String code, Instant createdAt) {

    public CashRegisterCreated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
