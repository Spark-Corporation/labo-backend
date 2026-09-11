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

import org.sparkcorporation.lims.catalog.api.MatrixId;

@RestController
@RequestMapping("/api/v1/catalog/matrices")
@RequiredArgsConstructor
public class MatrixAdminController {

    private final MatrixService service;

    @PostMapping
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Create a matrix",
            description =
                    "Registers a new sample matrix/specimen type (e.g. blood, urine) usable on"
                            + " analyses.")
    public ResponseEntity<MatrixResponse> create(@Valid @RequestBody CreateMatrixRequest req) {
        var resp = service.create(req);
        return ResponseEntity.created(URI.create("/api/v1/catalog/matrices/" + resp.id()))
                .body(resp);
    }

    @GetMapping
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Search matrices",
            description = "Lists sample matrices, paginated, optionally including disabled ones.")
    public MatrixSearchResponse search(
            @Parameter(description = "When true (default), excludes soft-deleted matrices.")
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
        Page<MatrixResponse> result = service.search(activeOnly, page, size);
        return new MatrixSearchResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @GetMapping("/{id}")
    @RequiresPermission("catalog.manage")
    @Operation(summary = "Get a matrix", description = "Returns a single sample matrix by its id.")
    public MatrixResponse get(
            @Parameter(description = "ULID of the matrix.") @PathVariable String id) {
        return service.get(new MatrixId(id));
    }

    @PatchMapping("/{id}")
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Update a matrix",
            description =
                    "Partially updates a matrix's name/description; null fields are left"
                            + " unchanged.")
    public MatrixResponse update(
            @Parameter(description = "ULID of the matrix.") @PathVariable String id,
            @Valid @RequestBody UpdateMatrixRequest req) {
        return service.update(new MatrixId(id), req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("catalog.manage")
    @Operation(summary = "Delete a matrix", description = "Soft-deletes the matrix.")
    public void delete(@Parameter(description = "ULID of the matrix.") @PathVariable String id) {
        service.delete(new MatrixId(id));
    }
}
