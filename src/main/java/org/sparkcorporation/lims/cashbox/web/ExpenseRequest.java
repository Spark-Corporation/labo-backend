package org.sparkcorporation.lims.cashbox.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.api.PaymentMode;

@Schema(description = "Body for recording a new cashbox expense")
public record ExpenseRequest(
        @Schema(description = "ULID of the (active) expense category") @NotBlank String categoryId,
        @Schema(description = "Payment mode used to pay the expense") @NotNull PaymentMode mode,
        @Schema(description = "Expense amount in XOF; must be strictly positive")
                @NotNull
                @DecimalMin(value = "0.01", message = "amount must be > 0")
                BigDecimal amount,
        @Schema(description = "Free-text description of the expense, if any")
                @Nullable String description,
        @Schema(description = "URI of an already-uploaded justification scan, if any")
                @Nullable String justificationScanUri,
        @Schema(description = "ULID of the cash register the expense is recorded against") @NotBlank
                String cashRegisterId) {}
