package org.sparkcorporation.lims.catalog.web;

import java.net.URI;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sparksam.securedx.rbac.RequiresPermission;
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
    public ResponseEntity<AnalysisResponse> create(@Valid @RequestBody CreateAnalysisRequest req) {
        var resp = service.create(mapper.toCommand(req));
        return ResponseEntity.created(URI.create("/api/v1/catalog/analyses/" + resp.id()))
                .body(resp);
    }

    @GetMapping
    @RequiresPermission("catalog.manage")
    public AnalysisSearchResponse list(
            @RequestParam(required = false) @Nullable String q,
            @RequestParam(name = "discipline", required = false) @Nullable String disciplineCode,
            @RequestParam(required = false) @Nullable AnalysisKind kind) {
        return service.search(q, disciplineCode, kind);
    }

    @GetMapping("/{id}")
    @RequiresPermission("catalog.manage")
    public AnalysisResponse get(@PathVariable String id) {
        return service.get(AnalysisId.of(id));
    }

    /** SP12: configuration-modification history (CDC Configurations §8/§9). */
    @GetMapping("/{id}/history")
    @RequiresPermission("catalog.view-history")
    public AnalysisHistoryResponse history(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return historyService.history(AnalysisId.of(id), page, size);
    }

    @PatchMapping("/{id}")
    @RequiresPermission("catalog.manage")
    public AnalysisResponse update(@PathVariable String id, @RequestBody Map<String, Object> body) {
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
    public void delete(@PathVariable String id) {
        service.delete(AnalysisId.of(id));
    }

    @PostMapping("/{id}:duplicate")
    @RequiresPermission("catalog.manage")
    public ResponseEntity<AnalysisResponse> duplicate(
            @PathVariable String id, @Valid @RequestBody DuplicateAnalysisRequest req) {
        var resp = service.duplicate(AnalysisId.of(id), req.newCode());
        return ResponseEntity.created(URI.create("/api/v1/catalog/analyses/" + resp.id()))
                .body(resp);
    }

    @PostMapping("/{id}:convert-to-grouped")
    @RequiresPermission("catalog.manage")
    public AnalysisResponse convertToGrouped(@PathVariable String id) {
        return service.convertToGrouped(AnalysisId.of(id));
    }

    @PostMapping("/{id}:convert-to-simple")
    @RequiresPermission("catalog.manage")
    public AnalysisResponse convertToSimple(@PathVariable String id) {
        return service.convertToSimple(AnalysisId.of(id));
    }

    @PostMapping("/{id}/components")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("catalog.manage")
    public AnalysisResponse addComponent(
            @PathVariable String id, @Valid @RequestBody AddComponentRequest req) {
        return service.addComponent(AnalysisId.of(id), req.childCode(), req.position());
    }

    @DeleteMapping("/{id}/components/{childCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("catalog.manage")
    public void removeComponent(@PathVariable String id, @PathVariable String childCode) {
        service.removeComponent(AnalysisId.of(id), childCode);
    }

    @PutMapping("/{id}/techniques")
    @RequiresPermission("catalog.manage")
    public AnalysisResponse putTechniques(
            @PathVariable String id, @Valid @RequestBody PutTechniquesRequest req) {
        return service.putTechniques(AnalysisId.of(id), req.items());
    }

    @PutMapping("/{id}/intervals")
    @RequiresPermission("catalog.manage")
    public List<ReferenceIntervalResponse> putIntervals(
            @PathVariable String id, @Valid @RequestBody PutIntervalsRequest req) {
        List<NewReferenceInterval> commands =
                req.items().stream().map(intervalMapper::toCommand).toList();
        return intervalService.replaceAll(AnalysisId.of(id), commands);
    }

    @PostMapping("/{id}/intervals:replicate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("catalog.manage")
    public void replicateIntervals(
            @PathVariable String id, @Valid @RequestBody ReplicateIntervalsRequest req) {
        intervalService.replicate(AnalysisId.of(id), req.sourceCategory(), req.targetCategories());
    }
}
