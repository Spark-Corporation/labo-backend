package org.sparkcorporation.lims.catalog.web;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.catalog.api.TechniqueId;

@RestController
@RequestMapping("/api/v1/catalog/techniques")
@RequiredArgsConstructor
public class TechniqueAdminController {

    private final TechniqueService service;

    @PostMapping
    @RequiresPermission("catalog.manage")
    public ResponseEntity<TechniqueResponse> create(
            @Valid @RequestBody CreateTechniqueRequest req) {
        var resp = service.create(req);
        return ResponseEntity.created(URI.create("/api/v1/catalog/techniques/" + resp.id()))
                .body(resp);
    }

    @GetMapping
    @RequiresPermission("catalog.manage")
    public TechniqueSearchResponse search(
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<TechniqueResponse> result = service.search(activeOnly, page, size);
        return new TechniqueSearchResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @GetMapping("/{id}")
    @RequiresPermission("catalog.manage")
    public TechniqueResponse get(@PathVariable String id) {
        return service.get(new TechniqueId(id));
    }

    @PatchMapping("/{id}")
    @RequiresPermission("catalog.manage")
    public TechniqueResponse update(
            @PathVariable String id, @Valid @RequestBody UpdateTechniqueRequest req) {
        return service.update(new TechniqueId(id), req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("catalog.manage")
    public void delete(@PathVariable String id) {
        service.delete(new TechniqueId(id));
    }
}
