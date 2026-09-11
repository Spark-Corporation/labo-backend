package org.sparkcorporation.lims.billing.web;

import java.time.Instant;
import java.time.LocalDate;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.billing.infra.AccountingReadModel;

/**
 * Read-only REST endpoints for CDC §VIII accounting recap (spec §11.2, plan chunk 8). All three
 * endpoints require {@code accounting.read} (granted to ADMIN + LAB_MANAGER per spec §9.5).
 *
 * <ul>
 *   <li>{@code GET /api/v1/accounting/daily?date=YYYY-MM-DD} — recap grouped by lab-file on a
 *       calendar day.
 *   <li>{@code GET /api/v1/accounting/by-payment-mode?from=…&to=…} — payment totals grouped by
 *       payment mode over a half-open instant window.
 *   <li>{@code GET /api/v1/accounting/outstanding} — current open balance split into {@code unpaid}
 *       and {@code refundsOwed}.
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/accounting")
@RequiredArgsConstructor
public class AccountingController {

    private final AccountingReadModel readModel;

    @GetMapping("/daily")
    @RequiresPermission("accounting.read")
    @Operation(
            summary = "Daily billing recap",
            description =
                    "Lists every lab file invoiced on the given calendar day, with billed, paid "
                            + "and remaining totals per file and summed across the day.")
    public AccountingDailyResponse daily(
            @Parameter(description = "Calendar day to recap, in the lab's local date (YYYY-MM-DD).")
                    @RequestParam("date")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate date) {
        return AccountingDailyResponse.from(readModel.daily(date));
    }

    @GetMapping("/by-payment-mode")
    @RequiresPermission("accounting.read")
    @Operation(
            summary = "Payments recap by payment mode",
            description =
                    "Sums payments received in the given half-open instant window, grouped by "
                            + "payment mode (cash, cheque, card, mobile money, …).")
    public AccountingByModeResponse byMode(
            @Parameter(description = "Start of the window (inclusive), as an ISO-8601 instant.")
                    @RequestParam("from")
                    Instant from,
            @Parameter(description = "End of the window (exclusive), as an ISO-8601 instant.")
                    @RequestParam("to")
                    Instant to) {
        return AccountingByModeResponse.from(readModel.byPaymentMode(from, to));
    }

    @GetMapping("/outstanding")
    @RequiresPermission("accounting.read")
    @Operation(
            summary = "Outstanding balances",
            description =
                    "Lists every lab file with a non-zero open balance, split into invoices the"
                        + " customer still owes (unpaid) and overpayments the lab still owes back "
                        + "(refundsOwed).")
    public AccountingOutstandingResponse outstanding() {
        return AccountingOutstandingResponse.from(readModel.outstanding());
    }
}
