package org.sparkcorporation.lims.billing.web;

import java.math.BigDecimal;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.infra.DailyRow;

/**
 * JSON response for {@code GET /api/v1/accounting/daily} (CDC §VIII). Wraps the per-lab-file rows
 * along with the summed totals across all rows.
 */
public record AccountingDailyResponse(List<FileRow> files, Totals totals) {

    /** One row in the daily recap — projected from {@link DailyRow}. */
    public record FileRow(
            String labFileId,
            String registrationNumber,
            @Nullable String customerName,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            BigDecimal remainingAmount) {}

    /** Sum of {@code totalAmount}, {@code paidAmount}, {@code remainingAmount} across all rows. */
    public record Totals(
            BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal remainingAmount) {}

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
