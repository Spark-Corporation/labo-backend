package org.sparkcorporation.lims.cashbox.web;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.api.PaymentMode;

@Schema(description = "A recorded cashbox expense")
public record ExpenseResponse(
        @Schema(description = "ULID of this expense") String id,
        @Schema(description = "ULID of the expense category") String categoryId,
        @Schema(description = "Payment mode used to pay the expense") PaymentMode mode,
        @Schema(description = "Expense amount, in XOF") BigDecimal amount,
        @Schema(description = "Free-text description of the expense, if any")
                @Nullable String description,
        @Schema(description = "URI of the linked justification scan, if any")
                @Nullable String justificationScanUri,
        @Schema(description = "ULID of the cash register the expense was recorded against")
                String cashRegisterId,
        @Schema(description = "User id who recorded the expense") String recordedBy,
        @Schema(description = "Timestamp the expense was recorded") Instant recordedAt) {}
