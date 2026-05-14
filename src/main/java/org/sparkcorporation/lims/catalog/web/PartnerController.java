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
    public ResponseEntity<PartnerResponse> create(@Valid @RequestBody PartnerRequest.Create req) {
        var summary = api.create(mapper.toCommand(req));
        var resp = mapper.toResponse(summary);
        return ResponseEntity.created(URI.create("/api/v1/partners/" + resp.id())).body(resp);
    }

    @GetMapping
    @RequiresPermission("lab-file.read")
    public PartnerSearchResponse list(
            @RequestParam(required = false) @Nullable String q,
            @RequestParam(required = false) @Nullable PartnerKind kind,
            @RequestParam(required = false) @Nullable Boolean disabled,
            Pageable pageable) {
        var page = api.search(q, kind, disabled, pageable);
        var items = page.map(mapper::toResponse).getContent();
        return new PartnerSearchResponse(items, page.getTotalElements());
    }

    @GetMapping("/{id}")
    @RequiresPermission("lab-file.read")
    public PartnerResponse get(@PathVariable String id) {
        var pid = PartnerId.of(id);
        return api.findById(pid)
                .map(mapper::toResponse)
                .orElseThrow(() -> new PartnerNotFoundException(pid));
    }

    @PutMapping("/{id}")
    @RequiresPermission("partner.manage")
    public PartnerResponse update(
            @PathVariable String id, @Valid @RequestBody PartnerRequest.Update req) {
        var summary = api.update(PartnerId.of(id), mapper.toCommand(req));
        return mapper.toResponse(summary);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("partner.manage")
    public void delete(@PathVariable String id) {
        api.disable(PartnerId.of(id));
    }
}
