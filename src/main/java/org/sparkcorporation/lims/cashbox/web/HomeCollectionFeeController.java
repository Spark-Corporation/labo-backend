package org.sparkcorporation.lims.cashbox.web;

import java.time.Instant;

import jakarta.validation.Valid;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileId;

@RestController
@RequestMapping("/api/v1/cashbox/home-collection-fees")
@RequiredArgsConstructor
public class HomeCollectionFeeController {

    private final HomeCollectionFeeService service;

    @GetMapping
    @RequiresPermission("cashbox.home-collection.read")
    @Operation(
            summary = "Search home-collection fees",
            description =
                    "Returns a paginated, filterable list of recorded home-collection fees (amounts"
                            + " in XOF), optionally restricted by recording-date range, lab file,"
                            + " recording user, or cash register.")
    public HomeCollectionFeeSearchResponse list(
            @RequestParam(required = false)
                    @Parameter(description = "Only fees recorded on or after this instant")
                    @Nullable Instant from,
            @RequestParam(required = false)
                    @Parameter(description = "Only fees recorded on or before this instant")
                    @Nullable Instant to,
            @RequestParam(required = false)
                    @Parameter(description = "Filter on the associated lab file's ULID")
                    @Nullable String labFileId,
            @RequestParam(required = false)
                    @Parameter(description = "Filter on the ULID of the user who recorded the fee")
                    @Nullable String recordedBy,
            @RequestParam(required = false)
                    @Parameter(description = "Filter on the cash register's ULID")
                    @Nullable String cashRegisterId,
            Pageable pageable) {
        var filters =
                new HomeCollectionFeeFilters(
                        from,
                        to,
                        labFileId == null ? null : new LabFileId(labFileId),
                        recordedBy == null ? null : new UserId(recordedBy),
                        cashRegisterId == null ? null : new CashRegisterId(cashRegisterId));
        var page = service.list(filters, pageable);
        return new HomeCollectionFeeSearchResponse(page.getContent(), page.getTotalElements());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("cashbox.home-collection.record")
    @Operation(
            summary = "Record a home-collection fee",
            description =
                    "Records the fee (in XOF) charged for collecting a sample at a patient's home,"
                        + " updates the lab file's home-collection-fee amount, and writes a"
                        + " matching cashbox journal entry. Rejected if the patient or lab file is"
                        + " unknown, the lab file is not OPEN, or the cash register is unknown.")
    public HomeCollectionFeeResponse create(@Valid @RequestBody HomeCollectionFeeRequest req) {
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
