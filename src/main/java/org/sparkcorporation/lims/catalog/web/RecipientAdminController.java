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

import org.sparkcorporation.lims.catalog.api.RecipientId;

@RestController
@RequestMapping("/api/v1/catalog/recipients")
@RequiredArgsConstructor
public class RecipientAdminController {

    private final RecipientService service;

    @PostMapping
    @RequiresPermission("catalog.manage")
    public ResponseEntity<RecipientResponse> create(
            @Valid @RequestBody CreateRecipientRequest req) {
        var resp = service.create(req);
        return ResponseEntity.created(URI.create("/api/v1/catalog/recipients/" + resp.id()))
                .body(resp);
    }

    @GetMapping
    @RequiresPermission("catalog.manage")
    public RecipientSearchResponse search(
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<RecipientResponse> result = service.search(activeOnly, page, size);
        return new RecipientSearchResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @GetMapping("/{id}")
    @RequiresPermission("catalog.manage")
    public RecipientResponse get(@PathVariable String id) {
        return service.get(new RecipientId(id));
    }

    @PatchMapping("/{id}")
    @RequiresPermission("catalog.manage")
    public RecipientResponse update(
            @PathVariable String id, @Valid @RequestBody UpdateRecipientRequest req) {
        return service.update(new RecipientId(id), req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("catalog.manage")
    public void delete(@PathVariable String id) {
        service.delete(new RecipientId(id));
    }
}
