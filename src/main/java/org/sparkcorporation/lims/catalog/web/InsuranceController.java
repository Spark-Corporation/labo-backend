package org.sparkcorporation.lims.catalog.web;

import java.net.URI;

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

import org.sparkcorporation.lims.catalog.api.InsuranceApi;
import org.sparkcorporation.lims.catalog.api.InsuranceId;
import org.sparkcorporation.lims.catalog.domain.InsuranceNotFoundException;

/**
 * REST endpoints for managing {@code Insurance}. Per billing spec §7.1: writes guarded by {@code
 * insurance.manage}; reads guarded by {@code lab-file.read} (1.6.a chunk-1 decision — catalog
 * configuration is readable by anyone authorized to read lab-files; see plan task 1.10).
 */
@RestController
@RequestMapping("/api/v1/insurances")
@RequiredArgsConstructor
public class InsuranceController {

    private final InsuranceApi api;
    private final InsuranceMapper mapper;

    @PostMapping
    @RequiresPermission("insurance.manage")
    @Operation(
            summary = "Create an insurance",
            description =
                    "Registers a new insurance payer: a coverageRate fraction of each patient"
                            + " invoice is billed to it at the given bRate (XOF per indiciary point"
                            + " B), the remainder stays payable by the patient.")
    public ResponseEntity<InsuranceResponse> create(
            @Valid @RequestBody InsuranceRequest.Create req) {
        var summary = api.create(mapper.toCommand(req));
        var resp = mapper.toResponse(summary);
        return ResponseEntity.created(URI.create("/api/v1/insurances/" + resp.id())).body(resp);
    }

    @GetMapping
    @RequiresPermission("lab-file.read")
    @Operation(
            summary = "Search insurances",
            description =
                    "Lists insurance payers matching an optional free-text query and"
                            + " disabled-status filter.")
    public InsuranceSearchResponse list(
            @Parameter(description = "Free-text search on the insurance code and name.")
                    @RequestParam(required = false)
                    @Nullable String q,
            @Parameter(
                            description =
                                    "Filter by disabled status; omit to return both active and"
                                            + " disabled insurances.")
                    @RequestParam(required = false)
                    @Nullable Boolean disabled,
            Pageable pageable) {
        var page = api.search(q, disabled, pageable);
        var items = page.map(mapper::toResponse).getContent();
        return new InsuranceSearchResponse(items, page.getTotalElements());
    }

    @GetMapping("/{id}")
    @RequiresPermission("lab-file.read")
    @Operation(
            summary = "Get an insurance",
            description = "Returns a single insurance payer by its id.")
    public InsuranceResponse get(
            @Parameter(description = "ULID of the insurance.") @PathVariable String id) {
        var iid = InsuranceId.of(id);
        return api.findById(iid)
                .map(mapper::toResponse)
                .orElseThrow(() -> new InsuranceNotFoundException(iid));
    }

    @PutMapping("/{id}")
    @RequiresPermission("insurance.manage")
    @Operation(
            summary = "Update an insurance",
            description =
                    "Updates an insurance's contact details and/or billing rates; null fields are"
                            + " left unchanged.")
    public InsuranceResponse update(
            @Parameter(description = "ULID of the insurance.") @PathVariable String id,
            @Valid @RequestBody InsuranceRequest.Update req) {
        var summary = api.update(InsuranceId.of(id), mapper.toCommand(req));
        return mapper.toResponse(summary);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("insurance.manage")
    @Operation(
            summary = "Disable an insurance",
            description =
                    "Soft-deletes (disables) the insurance; it is excluded from active listings and"
                            + " lookups thereafter.")
    public void delete(@Parameter(description = "ULID of the insurance.") @PathVariable String id) {
        api.disable(InsuranceId.of(id));
    }
}
