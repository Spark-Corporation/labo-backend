package org.sparkcorporation.lims.billing.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.api.PaymentMode;

/** Request body for {@code POST /api/v1/payments} (spec §7.4). */
@Schema(
        description =
                "Records a payment made against a lab file's invoice, through a given cash"
                        + " register.")
public record PaymentRequest(
        @Schema(description = "Id of the lab file the payment is for.") @NotBlank String labFileId,
        @Schema(description = "How the payment was made.") @NotNull PaymentMode mode,
        @Schema(description = "Amount paid, in XOF; must be strictly positive.")
                @NotNull
                @DecimalMin(value = "0.01", message = "amount must be > 0")
                BigDecimal amount,
        @Schema(description = "Id of the cash register the payment is recorded through.") @NotBlank
                String cashRegisterId,
        @Nullable
                @Schema(
                        description =
                                "Free-text note about the payment, e.g. a cheque or transaction"
                                        + " reference.")
                @Size(max = 500)
                String note) {}
