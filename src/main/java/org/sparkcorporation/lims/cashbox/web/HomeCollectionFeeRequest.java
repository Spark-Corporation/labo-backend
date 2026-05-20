package org.sparkcorporation.lims.cashbox.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.jspecify.annotations.Nullable;

public record HomeCollectionFeeRequest(
        @NotBlank String patientId,
        @NotBlank String labFileId,
        @NotNull @DecimalMin(value = "0.00", message = "distanceKm must be >= 0")
                BigDecimal distanceKm,
        @NotNull @DecimalMin(value = "0.01", message = "amount must be > 0") BigDecimal amount,
        @Nullable String receiptScanUri,
        @NotBlank String cashRegisterId) {}
