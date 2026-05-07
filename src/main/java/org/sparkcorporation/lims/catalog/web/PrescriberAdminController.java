package org.sparkcorporation.lims.catalog.web;

import java.net.URI;

import jakarta.validation.Valid;

import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
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

import org.sparkcorporation.lims.catalog.api.PrescriberId;

@RestController
@RequestMapping("/api/v1/catalog/prescribers")
@RequiredArgsConstructor
public class PrescriberAdminController {

    private final PrescriberService service;
    private final PrescriberMapper mapper;

    @PostMapping
    @RequiresPermission("prescriber.manage")
    public ResponseEntity<PrescriberResponse> create(
            @Valid @RequestBody CreatePrescriberRequest req) {
        var resp = service.create(mapper.toCommand(req));
        return ResponseEntity.created(URI.create("/api/v1/catalog/prescribers/" + resp.id()))
                .body(resp);
    }

    @GetMapping
    @RequiresPermission("prescriber.manage")
    public PrescriberSearchResponse list(
            @RequestParam(required = false) @Nullable String q,
            @RequestParam(name = "specialty", required = false) @Nullable String specialty) {
        return service.search(q, specialty);
    }

    @GetMapping("/{id}")
    @RequiresPermission("prescriber.manage")
    public PrescriberResponse get(@PathVariable String id) {
        return service.get(PrescriberId.of(id));
    }

    @PatchMapping("/{id}")
    @RequiresPermission("prescriber.manage")
    public PrescriberResponse update(
            @PathVariable String id, @Valid @RequestBody UpdatePrescriberRequest req) {
        // Spec §5.5: Prescriber `code` field in PATCH body is silently ignored (server-managed) —
        // not present on UpdatePrescriberRequest, so Jackson drops it on bind.
        return service.update(PrescriberId.of(id), mapper.toCommand(req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("prescriber.manage")
    public void delete(@PathVariable String id) {
        service.delete(PrescriberId.of(id));
    }
}
