package org.sparkcorporation.lims.billing.web;

import java.math.BigDecimal;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.api.PaymentMode;

/** JSON response shape for {@code Payment} endpoints (spec §7.4). */
public record PaymentResponse(
        String id,
        String labFileId,
        @Nullable String invoiceId,
        PaymentMode mode,
        BigDecimal amount,
        String cashRegisterId,
        String recordedByUserId,
        Instant recordedAt,
        @Nullable String note) {}
