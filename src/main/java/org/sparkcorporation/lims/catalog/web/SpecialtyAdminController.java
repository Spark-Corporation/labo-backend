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

import org.sparkcorporation.lims.catalog.api.SpecialtyId;
import org.sparkcorporation.lims.catalog.domain.UpdateSpecialty;

@RestController
@RequestMapping("/api/v1/catalog/specialties")
@RequiredArgsConstructor
public class SpecialtyAdminController {

    private final SpecialtyService service;
    private final SpecialtyMapper mapper;

    @PostMapping
    @RequiresPermission("prescriber.manage")
    public ResponseEntity<SpecialtyResponse> create(
            @Valid @RequestBody CreateSpecialtyRequest req) {
        var resp = service.create(mapper.toCommand(req));
        return ResponseEntity.created(URI.create("/api/v1/catalog/specialties/" + resp.id()))
                .body(resp);
    }

    @GetMapping
    @RequiresPermission("prescriber.manage")
    public SpecialtySearchResponse list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<SpecialtyResponse> result = service.list(page, size);
        return new SpecialtySearchResponse(result.getContent());
    }

    @GetMapping("/{id}")
    @RequiresPermission("prescriber.manage")
    public SpecialtyResponse get(@PathVariable String id) {
        return service.get(SpecialtyId.of(id));
    }

    /**
     * PATCH binds the body as a {@link Map} to enable an explicit immutable-{@code code} check
     * (spec §5.5): a {@code code} field on the body returns 422. Whitelisted keys are then mapped
     * onto {@link UpdateSpecialty}.
     */
    @PatchMapping("/{id}")
    @RequiresPermission("prescriber.manage")
    public SpecialtyResponse update(
            @PathVariable String id, @RequestBody Map<String, Object> body) {
        if (body.containsKey("code")) {
            throw new ImmutableCodeException(
                    "specialty code is immutable; remove the 'code' field from the PATCH body");
        }
        var cmd =
                new UpdateSpecialty(asString(body.get("name")), asString(body.get("description")));
        return service.update(SpecialtyId.of(id), cmd);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("prescriber.manage")
    public void delete(@PathVariable String id) {
        service.delete(SpecialtyId.of(id));
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
