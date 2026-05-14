package org.sparkcorporation.lims.billing.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Domain event — published after a {@code Payment} is persisted. Downstream consumers (accounting,
 * cash-box close-day) will subscribe in later phases; 1.6.a has no in-process listeners.
 */
public record PaymentRecorded(
        PaymentId id,
        LabFileId labFileId,
        PaymentMode mode,
        BigDecimal amount,
        Instant occurredAt) {

    public PaymentRecorded {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(labFileId, "labFileId");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
