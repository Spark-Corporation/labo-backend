package org.sparkcorporation.lims.billing.web;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.infra.DailyRow;

/**
 * JSON response for {@code GET /api/v1/accounting/daily} (CDC §VIII). Wraps the per-lab-file rows
 * along with the summed totals across all rows.
 */
@Schema(description = "Daily billing recap: one row per invoiced lab file, plus the summed totals.")
public record AccountingDailyResponse(
        @Schema(description = "One row per invoiced lab file in the requested window.")
                List<FileRow> files,
        @Schema(description = "Sum of the fields below across all rows.") Totals totals) {

    /** One row in the daily recap — projected from {@link DailyRow}. */
    @Schema(description = "Billing recap for a single lab file.")
    public record FileRow(
            @Schema(description = "Id of the lab file.") String labFileId,
            @Schema(description = "Lab file's registration number.") String registrationNumber,
            @Nullable @Schema(description = "Display name of the billed customer, if known.")
                    String customerName,
            @Schema(description = "Invoiced total, in XOF.") BigDecimal totalAmount,
            @Schema(description = "Sum of payments received so far, in XOF.") BigDecimal paidAmount,
            @Schema(
                            description =
                                    "totalAmount minus paidAmount, in XOF; negative when the"
                                            + " customer has overpaid.")
                    BigDecimal remainingAmount) {}

    /** Sum of {@code totalAmount}, {@code paidAmount}, {@code remainingAmount} across all rows. */
    @Schema(
            description =
                    "Sum of totalAmount, paidAmount and remainingAmount across a set of rows.")
    public record Totals(
            @Schema(description = "Sum of totalAmount, in XOF.") BigDecimal totalAmount,
            @Schema(description = "Sum of paidAmount, in XOF.") BigDecimal paidAmount,
            @Schema(description = "Sum of remainingAmount, in XOF.") BigDecimal remainingAmount) {}

    /** Build a response from native-SQL rows, computing totals server-side. */
    public static AccountingDailyResponse from(List<DailyRow> rows) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal paidAmount = BigDecimal.ZERO;
        BigDecimal remainingAmount = BigDecimal.ZERO;
        var files = new java.util.ArrayList<FileRow>(rows.size());
        for (var row : rows) {
            files.add(
                    new FileRow(
                            row.labFileId(),
                            row.registrationNumber(),
                            row.customerName(),
                            row.totalAmount(),
                            row.paidAmount(),
                            row.remainingAmount()));
            totalAmount = totalAmount.add(row.totalAmount());
            paidAmount = paidAmount.add(row.paidAmount());
            remainingAmount = remainingAmount.add(row.remainingAmount());
        }
        return new AccountingDailyResponse(
                List.copyOf(files), new Totals(totalAmount, paidAmount, remainingAmount));
    }
}
