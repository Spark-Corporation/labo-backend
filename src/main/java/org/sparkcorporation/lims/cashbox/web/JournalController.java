package org.sparkcorporation.lims.cashbox.web;

import java.time.Instant;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    @Operation(
            summary = "List cashbox journal entries",
            description =
                    "Returns a paginated, chronological journal of cashbox operations (payments,"
                        + " expenses, home-collection fees; amounts in XOF) within the given time"
                        + " range, optionally filtered by cash register or operation kind. Page"
                        + " size is capped at 200 regardless of what is requested.")
    public JournalResponse list(
            @RequestParam @Parameter(description = "Start of the time range (inclusive)")
                    Instant from,
            @RequestParam @Parameter(description = "End of the time range (inclusive)") Instant to,
            @RequestParam(required = false)
                    @Parameter(description = "Filter on the cash register's ULID")
                    @Nullable String cashRegisterId,
            @RequestParam(required = false)
                    @Parameter(
                            description =
                                    "Filter on the operation kind: PAYMENT, EXPENSE, or"
                                            + " HOME_COLLECTION_FEE")
                    @Nullable OperationKind operationKind,
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
