package org.sparkcorporation.lims.billing.web;

import java.time.Instant;
import java.time.LocalDate;

import dev.sparksam.securedx.rbac.RequiresPermission;
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
    public AccountingDailyResponse daily(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return AccountingDailyResponse.from(readModel.daily(date));
    }

    @GetMapping("/by-payment-mode")
    @RequiresPermission("accounting.read")
    public AccountingByModeResponse byMode(
            @RequestParam("from") Instant from, @RequestParam("to") Instant to) {
        return AccountingByModeResponse.from(readModel.byPaymentMode(from, to));
    }

    @GetMapping("/outstanding")
    @RequiresPermission("accounting.read")
    public AccountingOutstandingResponse outstanding() {
        return AccountingOutstandingResponse.from(readModel.outstanding());
    }
}
