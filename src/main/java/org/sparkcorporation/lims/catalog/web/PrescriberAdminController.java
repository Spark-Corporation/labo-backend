package org.sparkcorporation.lims.catalog.web;

import java.net.URI;

import jakarta.validation.Valid;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    @Operation(
            summary = "Create a prescriber",
            description =
                    "Registers a new prescribing physician with a server-generated code"
                            + " (PRS-NNNNNN), optionally linked to a medical specialty.")
    public ResponseEntity<PrescriberResponse> create(
            @Valid @RequestBody CreatePrescriberRequest req) {
        var resp = service.create(mapper.toCommand(req));
        return ResponseEntity.created(URI.create("/api/v1/catalog/prescribers/" + resp.id()))
                .body(resp);
    }

    @GetMapping
    @RequiresPermission("prescriber.manage")
    @Operation(
            summary = "Search prescribers",
            description =
                    "Lists prescribers matching an optional free-text query and/or specialty code"
                            + " filter.")
    public PrescriberSearchResponse list(
            @Parameter(description = "Free-text search on the prescriber code and name.")
                    @RequestParam(required = false)
                    @Nullable String q,
            @Parameter(description = "Restrict results to prescribers of this specialty's code.")
                    @RequestParam(name = "specialty", required = false)
                    @Nullable String specialty) {
        return service.search(q, specialty);
    }

    @GetMapping("/{id}")
    @RequiresPermission("prescriber.manage")
    @Operation(summary = "Get a prescriber", description = "Returns a single prescriber by its id.")
    public PrescriberResponse get(
            @Parameter(description = "ULID of the prescriber.") @PathVariable String id) {
        return service.get(PrescriberId.of(id));
    }

    @PatchMapping("/{id}")
    @RequiresPermission("prescriber.manage")
    @Operation(
            summary = "Update a prescriber",
            description =
                    "Partially updates a prescriber's contact/license details; null fields are"
                            + " left unchanged. The 'code' field is server-managed and silently"
                            + " ignored if present in the body.")
    public PrescriberResponse update(
            @Parameter(description = "ULID of the prescriber.") @PathVariable String id,
            @Valid @RequestBody UpdatePrescriberRequest req) {
        // Spec §5.5: Prescriber `code` field in PATCH body is silently ignored (server-managed) —
        // not present on UpdatePrescriberRequest, so Jackson drops it on bind.
        return service.update(PrescriberId.of(id), mapper.toCommand(req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("prescriber.manage")
    @Operation(summary = "Delete a prescriber", description = "Soft-deletes the prescriber.")
    public void delete(
            @Parameter(description = "ULID of the prescriber.") @PathVariable String id) {
        service.delete(PrescriberId.of(id));
    }
}
