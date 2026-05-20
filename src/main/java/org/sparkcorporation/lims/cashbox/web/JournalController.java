package org.sparkcorporation.lims.cashbox.web;

import java.time.Instant;

import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.cashbox.api.OperationKind;

@RestController
@RequestMapping("/api/v1/cashbox/journal")
@RequiredArgsConstructor
public class JournalController {

    private static final int MAX_PAGE_SIZE = 200;

    private final JournalService service;

    @GetMapping
    @RequiresPermission("cashbox.journal.read")
    public JournalResponse list(
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(required = false) @Nullable String cashRegisterId,
            @RequestParam(required = false) @Nullable OperationKind operationKind,
            @PageableDefault(size = 20) Pageable pageable) {
        var capped =
                pageable.getPageSize() > MAX_PAGE_SIZE
                        ? org.springframework.data.domain.PageRequest.of(
                                pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort())
                        : pageable;
        var filters =
                new JournalFilters(
                        cashRegisterId == null ? null : new CashRegisterId(cashRegisterId),
                        from,
                        to,
                        operationKind);
        return JournalResponse.from(service.findJournal(filters, capped));
    }
}
