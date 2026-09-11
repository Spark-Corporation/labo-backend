package org.sparkcorporation.lims.billing.web;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import org.sparkcorporation.lims.billing.infra.ByModeRow;

/**
 * JSON response for {@code GET /api/v1/accounting/by-payment-mode} (CDC §VIII). Lists one row per
 * distinct {@code mode} along with the summed totals. The {@code totals} record mirrors {@link
 * AccountingDailyResponse.Totals} shape so the UI can render any of the accounting endpoints
 * through the same totals widget. The {@code paidAmount} field reflects the windowed mode sum
 * (semantically the "money in"), while {@code remainingAmount} is {@code 0} (the by-mode view has
 * no notion of an outstanding balance).
 */
@Schema(description = "Payments received over the requested window, aggregated by payment mode.")
public record AccountingByModeResponse(
        @Schema(description = "One row per distinct payment mode used in the window.")
                List<ModeRow> rows,
        @Schema(
                        description =
                                "Totals across all rows; remainingAmount is always 0 here (this"
                                        + " view has no notion of an outstanding balance).")
                AccountingDailyResponse.Totals totals) {

    /** One row in the by-payment-mode aggregate — projected from {@link ByModeRow}. */
    @Schema(description = "Sum of payments made through a single payment mode.")
    public record ModeRow(
            @Schema(description = "Payment mode this row aggregates.") String mode,
            @Schema(description = "Sum of payment amounts for this mode, in XOF.")
                    BigDecimal totalAmount,
            @Schema(description = "Number of payments made through this mode.")
                    long paymentCount) {}

    /** Build a response from native-SQL rows, computing the {@code totalAmount} sum. */
    public static AccountingByModeResponse from(List<ByModeRow> rows) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        var mapped = new java.util.ArrayList<ModeRow>(rows.size());
        for (var row : rows) {
            mapped.add(new ModeRow(row.mode(), row.totalAmount(), row.paymentCount()));
            totalAmount = totalAmount.add(row.totalAmount());
        }
        return new AccountingByModeResponse(
                List.copyOf(mapped),
                new AccountingDailyResponse.Totals(totalAmount, totalAmount, BigDecimal.ZERO));
    }
}
