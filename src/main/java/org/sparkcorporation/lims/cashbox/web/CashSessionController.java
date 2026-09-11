package org.sparkcorporation.lims.cashbox.web;

import java.time.LocalDate;

import jakarta.validation.Valid;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.iam.api.UserId;

@RestController
@RequiredArgsConstructor
public class CashSessionController {

    private final CashSessionService service;

    @GetMapping("/api/v1/cashbox/sessions/{cashRegisterId}/{sessionDate}")
    @RequiresPermission("cashbox.journal.read")
    @Operation(
            summary = "Get a cash session",
            description =
                    "Returns the cash session for the given register and date, including its"
                            + " per-payment-mode totals (in XOF) once closed. Throws 404 if no"
                            + " session row exists yet for that register/date pair.")
    public CashSessionResponse get(
            @PathVariable @Parameter(description = "ULID of the cash register")
                    String cashRegisterId,
            @PathVariable
                    @Parameter(description = "Session's business date (ISO yyyy-MM-dd)")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate sessionDate) {
        return service.get(new CashRegisterId(cashRegisterId), sessionDate);
    }

    @PostMapping("/api/v1/cashbox/sessions/{cashRegisterId}/{sessionDate}:close")
    @RequiresPermission("cashbox.session.close")
    @Operation(
            summary = "Close a cash session",
            description =
                    "Computes and freezes the day's totals per payment mode (cash, cheque, card,"
                        + " Flooz, Mixx by Yas) plus expenses and home-collection fees, all in XOF,"
                        + " and marks the session CLOSED. Creates the session row first if this is"
                        + " the first close of the day for this register.")
    public CashSessionResponse close(
            @PathVariable @Parameter(description = "ULID of the cash register")
                    String cashRegisterId,
            @PathVariable
                    @Parameter(description = "Session's business date (ISO yyyy-MM-dd)")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate sessionDate) {
        return service.close(new CashRegisterId(cashRegisterId), sessionDate, callerId());
    }

    @PostMapping("/api/v1/cashbox/sessions/{cashRegisterId}/{sessionDate}:reopen")
    @RequiresPermission("cashbox.session.reopen")
    @Operation(
            summary = "Reopen a closed cash session",
            description =
                    "Flips a CLOSED session back to OPEN so corrections can be recorded, requiring"
                        + " a mandatory reason that is stored on the session for audit. Rejected if"
                        + " the session is not currently CLOSED.")
    public CashSessionResponse reopen(
            @PathVariable @Parameter(description = "ULID of the cash register")
                    String cashRegisterId,
            @PathVariable
                    @Parameter(description = "Session's business date (ISO yyyy-MM-dd)")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate sessionDate,
            @Valid @RequestBody ReopenRequest req) {
        return service.reopen(
                new CashRegisterId(cashRegisterId), sessionDate, req.reason(), callerId());
    }

    private static UserId callerId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new IllegalStateException("no authenticated user in SecurityContext");
        }
        return new UserId(auth.getName());
    }
}
