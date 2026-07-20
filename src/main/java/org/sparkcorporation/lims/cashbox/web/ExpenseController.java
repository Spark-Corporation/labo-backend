package org.sparkcorporation.lims.cashbox.web;

import java.time.Instant;

import jakarta.validation.Valid;

import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.cashbox.api.ExpenseCategoryId;
import org.sparkcorporation.lims.iam.api.UserId;

@RestController
@RequestMapping("/api/v1/cashbox/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService service;

    @GetMapping
    @RequiresPermission("cashbox.expense.read")
    public ExpenseSearchResponse list(
            @RequestParam(required = false) @Nullable Instant from,
            @RequestParam(required = false) @Nullable Instant to,
            @RequestParam(required = false) @Nullable String categoryId,
            @RequestParam(required = false) @Nullable String recordedBy,
            @RequestParam(required = false) @Nullable String cashRegisterId,
            Pageable pageable) {
        var filters =
                new ExpenseFilters(
                        from,
                        to,
                        categoryId == null ? null : new ExpenseCategoryId(categoryId),
                        recordedBy == null ? null : new UserId(recordedBy),
                        cashRegisterId == null ? null : new CashRegisterId(cashRegisterId));
        var page = service.list(filters, pageable);
        return new ExpenseSearchResponse(page.getContent(), page.getTotalElements());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("cashbox.expense.record")
    public ExpenseResponse create(@Valid @RequestBody ExpenseRequest req) {
        return service.record(req, callerId());
    }

    private static UserId callerId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new IllegalStateException("no authenticated user in SecurityContext");
        }
        return new UserId(auth.getName());
    }
}
