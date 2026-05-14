package org.sparkcorporation.lims.billing.web;

import java.math.BigDecimal;
import java.util.List;

import org.sparkcorporation.lims.billing.infra.OutstandingResult;
import org.sparkcorporation.lims.billing.infra.OutstandingRow;

/**
 * JSON response for {@code GET /api/v1/accounting/outstanding} (CDC §VIII). Splits open invoices
 * into two buckets and reports per-bucket totals so the UI can summarize "customer still owes" vs
 * "clinic still owes" at a glance.
 */
public record AccountingOutstandingResponse(
        List<AccountingDailyResponse.FileRow> unpaid,
        List<AccountingDailyResponse.FileRow> refundsOwed,
        AccountingDailyResponse.Totals unpaidTotals,
        AccountingDailyResponse.Totals refundsOwedTotals) {

    /** Build a response from native-SQL rows, computing both bucket totals. */
    public static AccountingOutstandingResponse from(OutstandingResult result) {
        var unpaidFiles = mapRows(result.unpaid());
        var refundsFiles = mapRows(result.refundsOwed());
        return new AccountingOutstandingResponse(
                unpaidFiles,
                refundsFiles,
                totalsOf(result.unpaid()),
                totalsOf(result.refundsOwed()));
    }

    private static List<AccountingDailyResponse.FileRow> mapRows(List<OutstandingRow> rows) {
        var out = new java.util.ArrayList<AccountingDailyResponse.FileRow>(rows.size());
        for (var row : rows) {
            out.add(
                    new AccountingDailyResponse.FileRow(
                            row.labFileId(),
                            row.registrationNumber(),
                            row.customerName(),
                            row.totalAmount(),
                            row.paidAmount(),
                            row.remainingAmount()));
        }
        return List.copyOf(out);
    }

    private static AccountingDailyResponse.Totals totalsOf(List<OutstandingRow> rows) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal remaining = BigDecimal.ZERO;
        for (var row : rows) {
            total = total.add(row.totalAmount());
            paid = paid.add(row.paidAmount());
            remaining = remaining.add(row.remainingAmount());
        }
        return new AccountingDailyResponse.Totals(total, paid, remaining);
    }
}
