package org.sparkcorporation.lims.catalog.web;

import java.net.URI;

import jakarta.validation.Valid;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.catalog.api.PreleveurId;

@RestController
@RequestMapping("/api/v1/catalog/preleveurs")
@RequiredArgsConstructor
public class PreleveurAdminController {

    private final PreleveurService service;
    private final PreleveurMapper mapper;

    @PostMapping
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Create a préleveur",
            description =
                    "Registers a new phlebotomist (préleveur) with a server-generated code"
                            + " (PHL-NNNNNN) and at least one phlebotomy-difficulty habilitation"
                            + " (CDC §5).")
    public ResponseEntity<PreleveurResponse> create(
            @Valid @RequestBody CreatePreleveurRequest req) {
        var resp = service.create(mapper.toCommand(req));
        return ResponseEntity.created(URI.create("/api/v1/catalog/preleveurs/" + resp.id()))
                .body(resp);
    }

    @GetMapping
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "List préleveurs",
            description = "Returns all phlebotomists (préleveurs) registered in the catalog.")
    public PreleveurSearchResponse list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Get a préleveur",
            description = "Returns a single phlebotomist (préleveur) by its id.")
    public PreleveurResponse get(
            @Parameter(description = "ULID of the préleveur.") @PathVariable String id) {
        return service.get(PreleveurId.of(id));
    }

    @PatchMapping("/{id}")
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Update a préleveur",
            description =
                    "Partially updates a préleveur's contact details and/or habilitations; null"
                            + " fields are left unchanged. The 'code' field is server-managed and"
                            + " silently ignored if present in the body.")
    public PreleveurResponse update(
            @Parameter(description = "ULID of the préleveur.") @PathVariable String id,
            @Valid @RequestBody UpdatePreleveurRequest req) {
        // Spec §5.5: Préleveur `code` field in PATCH body is silently ignored (server-managed) —
        // not present on UpdatePreleveurRequest, so Jackson drops it on bind.
        return service.update(PreleveurId.of(id), mapper.toCommand(req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("catalog.manage")
    @Operation(summary = "Delete a préleveur", description = "Soft-deletes the préleveur.")
    public void delete(@Parameter(description = "ULID of the préleveur.") @PathVariable String id) {
        service.delete(PreleveurId.of(id));
    }
}
