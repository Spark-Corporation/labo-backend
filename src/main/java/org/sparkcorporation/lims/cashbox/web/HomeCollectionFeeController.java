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
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileId;

@RestController
@RequestMapping("/api/v1/cashbox/home-collection-fees")
@RequiredArgsConstructor
public class HomeCollectionFeeController {

    private final HomeCollectionFeeService service;

    @GetMapping
    @RequiresPermission("cashbox.home-collection.read")
    public HomeCollectionFeeSearchResponse list(
            @RequestParam(required = false) @Nullable Instant from,
            @RequestParam(required = false) @Nullable Instant to,
            @RequestParam(required = false) @Nullable String labFileId,
            @RequestParam(required = false) @Nullable String recordedBy,
            @RequestParam(required = false) @Nullable String cashRegisterId,
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
