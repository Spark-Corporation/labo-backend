package org.sparkcorporation.lims.catalog.web;

import java.net.URI;

import jakarta.validation.Valid;

import dev.sparksam.securedx.rbac.RequiresPermission;
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
    public ResponseEntity<InsuranceResponse> create(
            @Valid @RequestBody InsuranceRequest.Create req) {
        var summary = api.create(mapper.toCommand(req));
        var resp = mapper.toResponse(summary);
        return ResponseEntity.created(URI.create("/api/v1/insurances/" + resp.id())).body(resp);
    }

    @GetMapping
    @RequiresPermission("lab-file.read")
    public InsuranceSearchResponse list(
            @RequestParam(required = false) @Nullable String q,
            @RequestParam(required = false) @Nullable Boolean disabled,
            Pageable pageable) {
        var page = api.search(q, disabled, pageable);
        var items = page.map(mapper::toResponse).getContent();
        return new InsuranceSearchResponse(items, page.getTotalElements());
    }

    @GetMapping("/{id}")
    @RequiresPermission("lab-file.read")
    public InsuranceResponse get(@PathVariable String id) {
        var iid = InsuranceId.of(id);
        return api.findById(iid)
                .map(mapper::toResponse)
                .orElseThrow(() -> new InsuranceNotFoundException(iid));
    }

    @PutMapping("/{id}")
    @RequiresPermission("insurance.manage")
    public InsuranceResponse update(
            @PathVariable String id, @Valid @RequestBody InsuranceRequest.Update req) {
        var summary = api.update(InsuranceId.of(id), mapper.toCommand(req));
        return mapper.toResponse(summary);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("insurance.manage")
    public void delete(@PathVariable String id) {
        api.disable(InsuranceId.of(id));
    }
}
