package org.sparkcorporation.lims.billing.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Domain event — published after a {@code Payment} is persisted.
 *
 * <p>Payload widened in sub-project 1.7.a to include {@code cashRegisterId} and {@code recordedBy}
 * so the cashbox journal listener can write a complete row from the event alone, without an extra
 * round-trip to {@link PaymentApi}.
 */
public record PaymentRecorded(
        PaymentId id,
        LabFileId labFileId,
        PaymentMode mode,
        BigDecimal amount,
        Instant occurredAt,
        CashRegisterId cashRegisterId,
        UserId recordedBy) {

    public PaymentRecorded {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(labFileId, "labFileId");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(cashRegisterId, "cashRegisterId");
        Objects.requireNonNull(recordedBy, "recordedBy");
    }
}
