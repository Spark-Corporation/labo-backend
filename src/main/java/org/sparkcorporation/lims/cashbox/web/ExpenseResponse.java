package org.sparkcorporation.lims.cashbox.web;

import java.math.BigDecimal;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.api.PaymentMode;

public record ExpenseResponse(
        String id,
        String categoryId,
        PaymentMode mode,
        BigDecimal amount,
        @Nullable String description,
        @Nullable String justificationScanUri,
        String cashRegisterId,
        String recordedBy,
        Instant recordedAt) {}
