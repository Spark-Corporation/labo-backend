package org.sparkcorporation.lims.cashbox.web;

import java.time.LocalDate;

import jakarta.validation.Valid;

import dev.sparksam.securedx.rbac.RequiresPermission;
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
    public CashSessionResponse get(
            @PathVariable String cashRegisterId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate) {
        return service.get(new CashRegisterId(cashRegisterId), sessionDate);
    }

    @PostMapping("/api/v1/cashbox/sessions/{cashRegisterId}/{sessionDate}:close")
    @RequiresPermission("cashbox.session.close")
    public CashSessionResponse close(
            @PathVariable String cashRegisterId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate) {
        return service.close(new CashRegisterId(cashRegisterId), sessionDate, callerId());
    }

    @PostMapping("/api/v1/cashbox/sessions/{cashRegisterId}/{sessionDate}:reopen")
    @RequiresPermission("cashbox.session.reopen")
    public CashSessionResponse reopen(
            @PathVariable String cashRegisterId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate,
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
