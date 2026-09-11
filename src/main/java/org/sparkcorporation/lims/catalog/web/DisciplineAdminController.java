package org.sparkcorporation.lims.catalog.web;

import java.net.URI;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
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

import org.sparkcorporation.lims.catalog.api.DisciplineId;

@RestController
@RequestMapping("/api/v1/catalog/disciplines")
@RequiredArgsConstructor
public class DisciplineAdminController {

    private final DisciplineService service;

    @PostMapping
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Create a discipline",
            description =
                    "Registers a new medical discipline (e.g. Biochimie, Hématologie), tying it to"
                            + " one of the five regulatory discipline prefixes used to build sample"
                            + " label ids.")
    public ResponseEntity<DisciplineResponse> create(
            @Valid @RequestBody CreateDisciplineRequest req) {
        var resp = service.create(req);
        return ResponseEntity.created(URI.create("/api/v1/catalog/disciplines/" + resp.id()))
                .body(resp);
    }

    @GetMapping
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Search disciplines",
            description = "Lists disciplines, paginated, optionally including disabled ones.")
    public DisciplineSearchResponse search(
            @Parameter(description = "When true (default), excludes soft-deleted disciplines.")
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
        Page<DisciplineResponse> result = service.search(activeOnly, page, size);
        return new DisciplineSearchResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @GetMapping("/{id}")
    @RequiresPermission("catalog.manage")
    @Operation(summary = "Get a discipline", description = "Returns a single discipline by its id.")
    public DisciplineResponse get(
            @Parameter(description = "ULID of the discipline.") @PathVariable String id) {
        return service.get(new DisciplineId(id));
    }

    /**
     * PATCH binds the body as a {@link Map} to enable immutable-field guards (mirrors {@link
     * SpecialtyAdminController}): a {@code disciplinePrefix} field on the body returns 422 because
     * the prefix is captured at create time and immutable thereafter (cf. spec §3.0 amendment #2).
     * Whitelisted keys ({@code name}, {@code description}) are then mapped onto {@link
     * UpdateDisciplineRequest}.
     */
    @PatchMapping("/{id}")
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Update a discipline",
            description =
                    "Partially updates a discipline's name/description; null fields are left"
                            + " unchanged. The 'disciplinePrefix' field is immutable — captured at"
                            + " create time — and the request is rejected with 422 if it is"
                            + " present in the body.")
    public DisciplineResponse update(
            @Parameter(description = "ULID of the discipline.") @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        if (body.containsKey("disciplinePrefix")) {
            throw new ImmutableDisciplinePrefixException(
                    "disciplinePrefix is immutable; remove the 'disciplinePrefix' field from the"
                            + " PATCH body");
        }
        var req =
                new UpdateDisciplineRequest(
                        asString(body.get("name")), asString(body.get("description")));
        return service.update(new DisciplineId(id), req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("catalog.manage")
    @Operation(summary = "Delete a discipline", description = "Soft-deletes the discipline.")
    public void delete(
            @Parameter(description = "ULID of the discipline.") @PathVariable String id) {
        service.delete(new DisciplineId(id));
    }

    private static @Nullable String asString(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        throw new IllegalArgumentException(
                "expected string value, got " + value.getClass().getSimpleName());
    }
}
