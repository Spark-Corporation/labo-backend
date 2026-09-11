package org.sparkcorporation.lims.catalog.web;

import java.net.URI;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.domain.NewReferenceInterval;

/**
 * Admin REST controller for {@link org.sparkcorporation.lims.catalog.domain.Analysis} per spec §5.5
 * — endpoints under {@code /api/v1/catalog/analyses/*}. The config-management endpoints are gated
 * by {@code @RequiresPermission("catalog.manage")}; the SP12 config-history read endpoint ({@code
 * GET /{id}/history}) is gated by {@code catalog.view-history}.
 *
 * <p>PATCH semantics (spec §5.5): {@code code} and {@code kind} are immutable. The PATCH endpoint
 * binds the body as a {@code Map} first to detect either field explicitly and reject with 422
 * before delegating to the typed {@link UpdateAnalysisRequest}.
 */
@RestController
@RequestMapping("/api/v1/catalog/analyses")
@RequiredArgsConstructor
public class AnalysisAdminController {

    private final AnalysisService service;
    private final ReferenceIntervalService intervalService;
    private final AnalysisHistoryService historyService;
    private final AnalysisMapper mapper;
    private final ReferenceIntervalMapper intervalMapper;
    private final ObjectMapper json;

    @PostMapping
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Create an analysis",
            description =
                    "Creates a new SIMPLE, GROUPED or SOCIETY_PANEL analysis definition in the"
                            + " catalog, resolving its discipline/matrix/recipient references.")
    public ResponseEntity<AnalysisResponse> create(@Valid @RequestBody CreateAnalysisRequest req) {
        var resp = service.create(mapper.toCommand(req));
        return ResponseEntity.created(URI.create("/api/v1/catalog/analyses/" + resp.id()))
                .body(resp);
    }

    @GetMapping
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Search analyses",
            description =
                    "Lists analyses matching the given free-text query, discipline and/or kind"
                            + " filters. Any filter left out matches all values.")
    public AnalysisSearchResponse list(
            @Parameter(description = "Free-text search on the analysis code and name.")
                    @RequestParam(required = false)
                    @Nullable String q,
            @Parameter(description = "Restrict results to analyses of this discipline's code.")
                    @RequestParam(name = "discipline", required = false)
                    @Nullable String disciplineCode,
            @Parameter(
                            description =
                                    "Restrict results to analyses of this kind (SIMPLE, GROUPED,"
                                            + " SOCIETY_PANEL).")
                    @RequestParam(required = false)
                    @Nullable AnalysisKind kind) {
        return service.search(q, disciplineCode, kind);
    }

    @GetMapping("/{id}")
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Get an analysis",
            description = "Returns the full configuration of a single analysis by its id.")
    public AnalysisResponse get(
            @Parameter(description = "ULID of the analysis.") @PathVariable String id) {
        return service.get(AnalysisId.of(id));
    }

    /** SP12: configuration-modification history (CDC Configurations §8/§9). */
    @GetMapping("/{id}/history")
    @RequiresPermission("catalog.view-history")
    @Operation(
            summary = "Get an analysis's configuration history",
            description =
                    "Returns the paginated, most-recent-first timeline of configuration changes"
                            + " (scalar fields, techniques, components, reference intervals)"
                            + " recorded for this analysis, including who made each change and"
                            + " when.")
    public AnalysisHistoryResponse history(
            @Parameter(description = "ULID of the analysis whose history to fetch.") @PathVariable
                    String id,
            @Parameter(description = "Zero-based page number.") @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(description = "Number of history entries per page.")
                    @RequestParam(defaultValue = "20")
                    int size) {
        return historyService.history(AnalysisId.of(id), page, size);
    }

    @PatchMapping("/{id}")
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Update an analysis",
            description =
                    "Partially updates an analysis's configuration; null fields are left"
                            + " unchanged. The 'code' and 'kind' fields are immutable and reject"
                            + " the request with 422 if present in the body — use the convert or"
                            + " duplicate endpoints to change them.")
    public AnalysisResponse update(
            @Parameter(description = "ULID of the analysis.") @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        if (body.containsKey("code")) {
            throw new ImmutableCodeException(
                    "analysis code is immutable; remove the 'code' field from the PATCH body");
        }
        if (body.containsKey("kind")) {
            throw new ImmutableKindException(
                    "analysis kind is immutable; use :convert-to-grouped or :convert-to-simple"
                            + " endpoints to change kind");
        }
        UpdateAnalysisRequest req = json.convertValue(body, UpdateAnalysisRequest.class);
        return service.update(AnalysisId.of(id), mapper.toCommand(req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Delete an analysis",
            description =
                    "Soft-deletes the analysis. Idempotent: deleting an already-deleted analysis is"
                            + " a no-op.")
    public void delete(@Parameter(description = "ULID of the analysis.") @PathVariable String id) {
        service.delete(AnalysisId.of(id));
    }

    @PostMapping("/{id}:duplicate")
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Duplicate an analysis",
            description =
                    "Clones the analysis (its components, technique links and reference"
                            + " intervals) under a new code; fails with 409 if the new code is"
                            + " already in use.")
    public ResponseEntity<AnalysisResponse> duplicate(
            @Parameter(description = "ULID of the analysis to duplicate.") @PathVariable String id,
            @Valid @RequestBody DuplicateAnalysisRequest req) {
        var resp = service.duplicate(AnalysisId.of(id), req.newCode());
        return ResponseEntity.created(URI.create("/api/v1/catalog/analyses/" + resp.id()))
                .body(resp);
    }

    @PostMapping("/{id}:convert-to-grouped")
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Convert an analysis to grouped",
            description =
                    "Switches the analysis kind from SIMPLE to GROUPED so it can hold other SIMPLE"
                            + " analyses as components; fails with 409 if it is already GROUPED or"
                            + " SOCIETY_PANEL.")
    public AnalysisResponse convertToGrouped(
            @Parameter(description = "ULID of the analysis.") @PathVariable String id) {
        return service.convertToGrouped(AnalysisId.of(id));
    }

    @PostMapping("/{id}:convert-to-simple")
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Convert an analysis to simple",
            description =
                    "Switches the analysis kind to SIMPLE; fails with 409 if it is already SIMPLE"
                            + " or still has components attached.")
    public AnalysisResponse convertToSimple(
            @Parameter(description = "ULID of the analysis.") @PathVariable String id) {
        return service.convertToSimple(AnalysisId.of(id));
    }

    @PostMapping("/{id}/components")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Add a component analysis",
            description =
                    "Attaches an active SIMPLE analysis as a child component of this GROUPED or"
                            + " SOCIETY_PANEL analysis at the given position.")
    public AnalysisResponse addComponent(
            @Parameter(description = "ULID of the parent (GROUPED/SOCIETY_PANEL) analysis.")
                    @PathVariable
                    String id,
            @Valid @RequestBody AddComponentRequest req) {
        return service.addComponent(AnalysisId.of(id), req.childCode(), req.position());
    }

    @DeleteMapping("/{id}/components/{childCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Remove a component analysis",
            description =
                    "Detaches a child analysis from this grouped analysis and renumbers the"
                            + " remaining components' positions densely; a no-op if the child is"
                            + " not currently linked.")
    public void removeComponent(
            @Parameter(description = "ULID of the parent analysis.") @PathVariable String id,
            @Parameter(description = "Code of the child analysis to detach.") @PathVariable
                    String childCode) {
        service.removeComponent(AnalysisId.of(id), childCode);
    }

    @PutMapping("/{id}/techniques")
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Replace an analysis's techniques",
            description =
                    "Replaces the full set of technique links for the analysis. At most one link"
                            + " may be marked default, otherwise the request fails with 422.")
    public AnalysisResponse putTechniques(
            @Parameter(description = "ULID of the analysis.") @PathVariable String id,
            @Valid @RequestBody PutTechniquesRequest req) {
        return service.putTechniques(AnalysisId.of(id), req.items());
    }

    @PutMapping("/{id}/intervals")
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Replace an analysis's reference intervals",
            description =
                    "Replaces all reference intervals for the analysis: existing intervals are"
                            + " soft-deleted and the given ones persisted in their place.")
    public List<ReferenceIntervalResponse> putIntervals(
            @Parameter(description = "ULID of the analysis.") @PathVariable String id,
            @Valid @RequestBody PutIntervalsRequest req) {
        List<NewReferenceInterval> commands =
                req.items().stream().map(intervalMapper::toCommand).toList();
        return intervalService.replaceAll(AnalysisId.of(id), commands);
    }

    @PostMapping("/{id}/intervals:replicate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("catalog.manage")
    @Operation(
            summary = "Replicate reference intervals across patient categories",
            description =
                    "Copies the reference intervals defined for one patient category onto one or"
                            + " more target categories, overwriting any intervals already defined"
                            + " there. When no targets are given, defaults to the other three"
                            + " patient categories. Only applies to SIMPLE analyses.")
    public void replicateIntervals(
            @Parameter(description = "ULID of the analysis.") @PathVariable String id,
            @Valid @RequestBody ReplicateIntervalsRequest req) {
        intervalService.replicate(AnalysisId.of(id), req.sourceCategory(), req.targetCategories());
    }
}
