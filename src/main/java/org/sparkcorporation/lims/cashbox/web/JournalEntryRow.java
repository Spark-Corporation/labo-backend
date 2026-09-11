package org.sparkcorporation.lims.cashbox.web;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.api.PaymentMode;
import org.sparkcorporation.lims.cashbox.api.OperationKind;

@Schema(
        description =
                "One cash-flow operation recorded through a cash register: a payment, an "
                        + "expense, or a home-collection fee.")
public record JournalEntryRow(
        @Schema(description = "When the operation was recorded.") Instant occurredAt,
        @Schema(description = "Kind of operation: PAYMENT, EXPENSE, or HOME_COLLECTION_FEE.")
                OperationKind operationKind,
        @Schema(description = "Id of the user who recorded the operation.") String userId,
        @Nullable @Schema(description = "Display name of the user who recorded the operation.")
                String userLabel,
        @Nullable
                @Schema(
                        description =
                                "Id of the related lab file, for PAYMENT/HOME_COLLECTION_FEE"
                                        + " operations.")
                String labFileId,
        @Nullable @Schema(description = "Registration number of the related lab file, if any.")
                String labFileNumber,
        @Nullable @Schema(description = "Id of the related patient, if any.") String patientId,
        @Nullable @Schema(description = "Display name of the related patient, if any.")
                String patientLabel,
        @Nullable @Schema(description = "How the operation was paid, for PAYMENT operations.")
                PaymentMode paymentMode,
        @Schema(description = "Amount of this operation, in XOF.") BigDecimal amount,
        @Nullable
                @Schema(
                        description =
                                "Total amount of the related invoice, for PAYMENT " + "operations.")
                BigDecimal invoiceTotal,
        @Nullable
                @Schema(
                        description =
                                "Amount paid so far on the related invoice, including "
                                        + "this operation, for PAYMENT operations.")
                BigDecimal paidSoFar,
        @Nullable
                @Schema(
                        description =
                                "Remaining balance on the related invoice after this "
                                        + "operation, for PAYMENT operations.")
                BigDecimal remaining,
        @Schema(description = "Id of the cash register the operation was recorded through.")
                String cashRegisterId) {}
