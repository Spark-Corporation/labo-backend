package org.sparkcorporation.lims.billing.web;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.api.PaymentMode;

/** JSON response shape for {@code Payment} endpoints (spec §7.4). */
@Schema(description = "A payment recorded against a lab file's invoice.")
public record PaymentResponse(
        @Schema(description = "Payment id.") String id,
        @Schema(description = "Id of the lab file the payment is for.") String labFileId,
        @Nullable
                @Schema(
                        description =
                                "Id of the invoice this payment applies to; absent if the"
                                        + " invoice has not been issued yet.")
                String invoiceId,
        @Schema(description = "How the payment was made.") PaymentMode mode,
        @Schema(description = "Amount paid, in XOF.") BigDecimal amount,
        @Schema(description = "Id of the cash register the payment was recorded through.")
                String cashRegisterId,
        @Schema(description = "Id of the user who recorded the payment.") String recordedByUserId,
        @Schema(description = "When the payment was recorded.") Instant recordedAt,
        @Nullable
                @Schema(
                        description =
                                "Free-text note about the payment, e.g. a cheque or transaction"
                                        + " reference.")
                String note) {}
