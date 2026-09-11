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

import org.sparkcorporation.lims.catalog.api.AntibioticId;

@RestController
@RequestMapping("/api/v1/catalog/antibiotics")
@RequiredArgsConstructor
public class AntibioticAdminController {

    private final AntibioticService service;

    @PostMapping
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Create an antibiotic",
            description =
                    "Registers a new antibiotic catalog entry, including its antibiogram"
                            + " reference/intermediate diameter thresholds.")
    public ResponseEntity<AntibioticResponse> create(
            @Valid @RequestBody CreateAntibioticRequest req) {
        var resp = service.create(req);
        return ResponseEntity.created(URI.create("/api/v1/catalog/antibiotics/" + resp.id()))
                .body(resp);
    }

    @GetMapping
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Search antibiotics",
            description = "Lists antibiotics, paginated, optionally including disabled ones.")
    public AntibioticSearchResponse search(
            @Parameter(description = "When true (default), excludes soft-deleted antibiotics.")
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
        Page<AntibioticResponse> result = service.search(activeOnly, page, size);
        return new AntibioticSearchResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @GetMapping("/{id}")
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Get an antibiotic",
            description = "Returns a single antibiotic by its id.")
    public AntibioticResponse get(
            @Parameter(description = "ULID of the antibiotic.") @PathVariable String id) {
        return service.get(new AntibioticId(id));
    }

    @PatchMapping("/{id}")
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Update an antibiotic",
            description = "Partially updates an antibiotic; null fields are left unchanged.")
    public AntibioticResponse update(
            @Parameter(description = "ULID of the antibiotic.") @PathVariable String id,
            @Valid @RequestBody UpdateAntibioticRequest req) {
        return service.update(new AntibioticId(id), req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("catalog.manage")
    @Operation(summary = "Delete an antibiotic", description = "Soft-deletes the antibiotic.")
    public void delete(
            @Parameter(description = "ULID of the antibiotic.") @PathVariable String id) {
        service.delete(new AntibioticId(id));
    }
}
