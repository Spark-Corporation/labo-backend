package org.sparkcorporation.lims.cashbox.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.api.PaymentMode;

public record ExpenseRequest(
        @NotBlank String categoryId,
        @NotNull PaymentMode mode,
        @NotNull @DecimalMin(value = "0.01", message = "amount must be > 0") BigDecimal amount,
        @Nullable String description,
        @Nullable String justificationScanUri,
        @NotBlank String cashRegisterId) {}
