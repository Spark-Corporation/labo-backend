package org.sparkcorporation.lims.catalog.web;

import java.net.URI;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import dev.sparksam.securedx.rbac.RequiresPermission;
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
    public ResponseEntity<DisciplineResponse> create(
            @Valid @RequestBody CreateDisciplineRequest req) {
        var resp = service.create(req);
        return ResponseEntity.created(URI.create("/api/v1/catalog/disciplines/" + resp.id()))
                .body(resp);
    }

    @GetMapping
    @RequiresPermission("catalog.manage")
    public DisciplineSearchResponse search(
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
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
    public DisciplineResponse get(@PathVariable String id) {
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
    public DisciplineResponse update(
            @PathVariable String id, @RequestBody Map<String, Object> body) {
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
    public void delete(@PathVariable String id) {
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
