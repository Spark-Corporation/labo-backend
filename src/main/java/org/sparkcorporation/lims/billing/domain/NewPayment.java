package org.sparkcorporation.lims.billing.domain;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.api.PaymentMode;

/**
 * Command record for {@link Payment#record(NewPayment, java.time.Clock)}. Immutable input bundle —
 * the static factory copies fields into the aggregate, then validates invariants (positive amount,
 * mode present, etc.).
 */
public record NewPayment(
        String labFileId,
        PaymentMode mode,
        BigDecimal amount,
        String cashRegisterId,
        String recordedByUserId,
        @Nullable String note) {}
