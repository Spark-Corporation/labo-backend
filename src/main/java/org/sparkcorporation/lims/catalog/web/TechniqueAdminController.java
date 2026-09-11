package org.sparkcorporation.lims.catalog.web;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    @Operation(
            summary = "Create a technique",
            description =
                    "Registers a new analytical technique that can be linked to one or more"
                            + " analyses.")
    public ResponseEntity<TechniqueResponse> create(
            @Valid @RequestBody CreateTechniqueRequest req) {
        var resp = service.create(req);
        return ResponseEntity.created(URI.create("/api/v1/catalog/techniques/" + resp.id()))
                .body(resp);
    }

    @GetMapping
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Search techniques",
            description =
                    "Lists analytical techniques, paginated, optionally including disabled ones.")
    public TechniqueSearchResponse search(
            @Parameter(description = "When true (default), excludes soft-deleted techniques.")
                    @RequestParam(defaultValue = "true")
                    boolean activeOnly,
            @Parameter(description = "Zero-based page number.")
                    @RequestParam(defaultValue = "0")
                    @Min(0)
                    int page,
            @Parameter(description = "Page size (1-100).")
                    @RequestParam(defaultValue = "20")
                    @Min(1)
                    @Max(100)
                    int size) {
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
    @Operation(
            summary = "Get a technique",
            description = "Returns a single analytical technique by its id.")
    public TechniqueResponse get(
            @Parameter(description = "ULID of the technique.") @PathVariable String id) {
        return service.get(new TechniqueId(id));
    }

    @PatchMapping("/{id}")
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Update a technique",
            description =
                    "Partially updates a technique's name/description; null fields are left"
                            + " unchanged.")
    public TechniqueResponse update(
            @Parameter(description = "ULID of the technique.") @PathVariable String id,
            @Valid @RequestBody UpdateTechniqueRequest req) {
        return service.update(new TechniqueId(id), req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("catalog.manage")
    @Operation(summary = "Delete a technique", description = "Soft-deletes the technique.")
    public void delete(@Parameter(description = "ULID of the technique.") @PathVariable String id) {
        service.delete(new TechniqueId(id));
    }
}
