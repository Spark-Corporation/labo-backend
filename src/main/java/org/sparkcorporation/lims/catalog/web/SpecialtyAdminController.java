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
    @Operation(
            summary = "Create a specialty",
            description = "Registers a new medical specialty that prescribers can be tagged with.")
    public ResponseEntity<SpecialtyResponse> create(
            @Valid @RequestBody CreateSpecialtyRequest req) {
        var resp = service.create(mapper.toCommand(req));
        return ResponseEntity.created(URI.create("/api/v1/catalog/specialties/" + resp.id()))
                .body(resp);
    }

    @GetMapping
    @RequiresPermission("prescriber.manage")
    @Operation(summary = "List specialties", description = "Lists medical specialties, paginated.")
    public SpecialtySearchResponse list(
            @Parameter(description = "Zero-based page number.")
                    @RequestParam(defaultValue = "0")
                    @Min(0)
                    int page,
            @Parameter(description = "Page size (1-100).")
                    @RequestParam(defaultValue = "20")
                    @Min(1)
                    @Max(100)
                    int size) {
        Page<SpecialtyResponse> result = service.list(page, size);
        return new SpecialtySearchResponse(result.getContent());
    }

    @GetMapping("/{id}")
    @RequiresPermission("prescriber.manage")
    @Operation(
            summary = "Get a specialty",
            description = "Returns a single medical specialty by its id.")
    public SpecialtyResponse get(
            @Parameter(description = "ULID of the specialty.") @PathVariable String id) {
        return service.get(SpecialtyId.of(id));
    }

    /**
     * PATCH binds the body as a {@link Map} to enable an explicit immutable-{@code code} check
     * (spec §5.5): a {@code code} field on the body returns 422. Whitelisted keys are then mapped
     * onto {@link UpdateSpecialty}.
     */
    @PatchMapping("/{id}")
    @RequiresPermission("prescriber.manage")
    @Operation(
            summary = "Update a specialty",
            description =
                    "Partially updates a specialty's name/description; null fields are left"
                            + " unchanged. The 'code' field is immutable and the request is"
                            + " rejected with 422 if it is present in the body.")
    public SpecialtyResponse update(
            @Parameter(description = "ULID of the specialty.") @PathVariable String id,
            @RequestBody Map<String, Object> body) {
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
    @Operation(summary = "Delete a specialty", description = "Soft-deletes the specialty.")
    public void delete(@Parameter(description = "ULID of the specialty.") @PathVariable String id) {
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
