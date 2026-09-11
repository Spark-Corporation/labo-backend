package org.sparkcorporation.lims.billing.web;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.validation.Valid;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.billing.api.CashRegisterApi;
import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.billing.domain.CashRegisterNotFoundException;
import org.sparkcorporation.lims.iam.api.UserId;

/**
 * REST endpoints for {@code CashRegister} (spec §7.3). All 6 endpoints require {@code
 * cash-register.manage} (LAB_MANAGER per CDC §9.5).
 */
@RestController
@RequestMapping("/api/v1/cash-registers")
@RequiredArgsConstructor
public class CashRegisterController {

    private final CashRegisterApi api;
    private final CashRegisterMapper mapper;

    @PostMapping
    @RequiresPermission("cash-register.manage")
    @Operation(
            summary = "Create a cash register",
            description = "Creates a new cash register that payments can be recorded against.")
    public ResponseEntity<CashRegisterResponse> create(
            @Valid @RequestBody CashRegisterRequest.Create req) {
        var summary = api.create(mapper.toCommand(req));
        var resp = mapper.toResponse(summary);
        return ResponseEntity.created(URI.create("/api/v1/cash-registers/" + resp.id())).body(resp);
    }

    @GetMapping
    @RequiresPermission("cash-register.manage")
    @Operation(
            summary = "Search cash registers",
            description =
                    "Lists cash registers, optionally filtered by free-text query and "
                            + "disabled state.")
    public CashRegisterSearchResponse list(
            @Parameter(description = "Free-text search on code/label; omit to match all.")
                    @RequestParam(required = false)
                    @Nullable String q,
            @Parameter(description = "Filter by disabled state; omit to include both.")
                    @RequestParam(required = false)
                    @Nullable Boolean disabled,
            Pageable pageable) {
        var page = api.search(q, disabled, pageable);
        var items = page.map(mapper::toResponse).getContent();
        return new CashRegisterSearchResponse(items, page.getTotalElements());
    }

    @GetMapping("/{id}")
    @RequiresPermission("cash-register.manage")
    @Operation(
            summary = "Get a cash register",
            description = "Fetches a single cash register by id.")
    public CashRegisterResponse get(@PathVariable String id) {
        var cid = CashRegisterId.of(id);
        return api.findById(cid)
                .map(mapper::toResponse)
                .orElseThrow(() -> new CashRegisterNotFoundException(cid));
    }

    @PutMapping("/{id}")
    @RequiresPermission("cash-register.manage")
    @Operation(
            summary = "Rename a cash register",
            description =
                    "Updates a cash register's label. code and sequenceNumber are "
                            + "immutable and cannot be changed here.")
    public CashRegisterResponse update(
            @PathVariable String id, @Valid @RequestBody CashRegisterRequest.Update req) {
        var summary = api.update(CashRegisterId.of(id), mapper.toCommand(req));
        return mapper.toResponse(summary);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("cash-register.manage")
    @Operation(
            summary = "Disable a cash register",
            description =
                    "Soft-disables a cash register so it can no longer be used for new "
                            + "payments; existing payments recorded through it are unaffected.")
    public void delete(@PathVariable String id) {
        api.disable(CashRegisterId.of(id));
    }

    @PutMapping("/{id}/users")
    @RequiresPermission("cash-register.manage")
    @Operation(
            summary = "Assign users to a cash register",
            description =
                    "Replaces the full set of users allowed to record payments through "
                            + "this register.")
    public CashRegisterResponse assignUsers(
            @PathVariable String id, @Valid @RequestBody CashRegisterUserAssignmentRequest req) {
        var cid = CashRegisterId.of(id);
        Set<UserId> typed = new LinkedHashSet<>();
        for (String uid : req.userIds()) {
            typed.add(new UserId(uid));
        }
        api.assignUsers(cid, typed);
        var summary = api.findById(cid).orElseThrow(() -> new CashRegisterNotFoundException(cid));
        return mapper.toResponse(summary);
    }
}
