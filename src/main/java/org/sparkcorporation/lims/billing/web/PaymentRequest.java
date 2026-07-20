package org.sparkcorporation.lims.billing.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.api.PaymentMode;

/** Request body for {@code POST /api/v1/payments} (spec §7.4). */
public record PaymentRequest(
        @NotBlank String labFileId,
        @NotNull PaymentMode mode,
        @NotNull @DecimalMin(value = "0.01", message = "amount must be > 0") BigDecimal amount,
        @NotBlank String cashRegisterId,
        @Nullable @Size(max = 500) String note) {}
