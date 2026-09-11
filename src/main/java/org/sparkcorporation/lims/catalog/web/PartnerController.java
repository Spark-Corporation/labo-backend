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

import org.sparkcorporation.lims.catalog.api.PartnerApi;
import org.sparkcorporation.lims.catalog.api.PartnerId;
import org.sparkcorporation.lims.catalog.api.PartnerKind;
import org.sparkcorporation.lims.catalog.domain.PartnerNotFoundException;

@RestController
@RequestMapping("/api/v1/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerApi api;
    private final PartnerMapper mapper;

    @PostMapping
    @RequiresPermission("partner.manage")
    @Operation(
            summary = "Create a partner",
            description =
                    "Registers a new corporate/institutional partner (society, partner lab or"
                            + " health center) that bills the entire patient invoice at the given"
                            + " bRate (XOF per indiciary point B), unlike an insurance which only"
                            + " covers a fraction.")
    public ResponseEntity<PartnerResponse> create(@Valid @RequestBody PartnerRequest.Create req) {
        var summary = api.create(mapper.toCommand(req));
        var resp = mapper.toResponse(summary);
        return ResponseEntity.created(URI.create("/api/v1/partners/" + resp.id())).body(resp);
    }

    @GetMapping
    @RequiresPermission("lab-file.read")
    @Operation(
            summary = "Search partners",
            description =
                    "Lists partners matching an optional free-text query, kind and/or"
                            + " disabled-status filter.")
    public PartnerSearchResponse list(
            @Parameter(description = "Free-text search on the partner code and name.")
                    @RequestParam(required = false)
                    @Nullable String q,
            @Parameter(
                            description =
                                    "Restrict results to this partner kind (SOCIETY, PARTNER_LAB,"
                                            + " HEALTH_CENTER).")
                    @RequestParam(required = false)
                    @Nullable PartnerKind kind,
            @Parameter(
                            description =
                                    "Filter by disabled status; omit to return both active and"
                                            + " disabled partners.")
                    @RequestParam(required = false)
                    @Nullable Boolean disabled,
            Pageable pageable) {
        var page = api.search(q, kind, disabled, pageable);
        var items = page.map(mapper::toResponse).getContent();
        return new PartnerSearchResponse(items, page.getTotalElements());
    }

    @GetMapping("/{id}")
    @RequiresPermission("lab-file.read")
    @Operation(summary = "Get a partner", description = "Returns a single partner by its id.")
    public PartnerResponse get(
            @Parameter(description = "ULID of the partner.") @PathVariable String id) {
        var pid = PartnerId.of(id);
        return api.findById(pid)
                .map(mapper::toResponse)
                .orElseThrow(() -> new PartnerNotFoundException(pid));
    }

    @PutMapping("/{id}")
    @RequiresPermission("partner.manage")
    @Operation(
            summary = "Update a partner",
            description =
                    "Updates a partner's kind, contact details and/or bRate; null fields are left"
                            + " unchanged.")
    public PartnerResponse update(
            @Parameter(description = "ULID of the partner.") @PathVariable String id,
            @Valid @RequestBody PartnerRequest.Update req) {
        var summary = api.update(PartnerId.of(id), mapper.toCommand(req));
        return mapper.toResponse(summary);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("partner.manage")
    @Operation(
            summary = "Disable a partner",
            description =
                    "Soft-deletes (disables) the partner; it is excluded from active listings and"
                            + " lookups thereafter.")
    public void delete(@Parameter(description = "ULID of the partner.") @PathVariable String id) {
        api.disable(PartnerId.of(id));
    }
}
