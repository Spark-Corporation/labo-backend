package org.sparkcorporation.lims.order.web;

import jakarta.validation.Valid;

import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Admin REST controller for sample recording per spec §7.1. Three colon-action endpoints, one per
 * sample sub-type. Each returns 201 with the created {@link SampleResponse} body and <em>no</em>
 * Location header (no GET sub-resource exposed in 1.4 — the parent {@code GET /lab-files/{id}}
 * surfaces the full sample list).
 *
 * <p>The colon-action ({@code samples:in-lab} / {@code :home} / {@code :external}) is rendered at
 * the method level rather than the class level so Spring's URL pattern matcher does not confuse the
 * trailing {@code :name} fragment with a path-segment continuation. Mirrors the catalog convention
 * ({@code /{id}/intervals:replicate}).
 */
@RestController
@RequestMapping("/api/v1/lab-files")
@RequiredArgsConstructor
public class SampleAdminController {

    private final SampleService service;

    @PostMapping("/{labFileId}/samples:in-lab")
    @RequiresPermission("sample.record-in-lab")
    public ResponseEntity<SampleResponse> recordInLab(
            @PathVariable String labFileId,
            @Valid @RequestBody RecordInLabSampleRequest req,
            @Nullable Authentication authentication) {
        var resp =
                service.recordInLab(new LabFileId(labFileId), req, currentUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/{labFileId}/samples:home")
    @RequiresPermission("sample.record-home")
    public ResponseEntity<SampleResponse> recordHome(
            @PathVariable String labFileId,
            @Valid @RequestBody RecordHomeSampleRequest req,
            @Nullable Authentication authentication) {
        var resp = service.recordHome(new LabFileId(labFileId), req, currentUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/{labFileId}/samples:external")
    @RequiresPermission("sample.record-external")
    public ResponseEntity<SampleResponse> recordExternal(
            @PathVariable String labFileId,
            @Valid @RequestBody RecordExternalSampleRequest req,
            @Nullable Authentication authentication) {
        var resp =
                service.recordExternal(
                        new LabFileId(labFileId), req, currentUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    private static @Nullable String currentUserId(@Nullable Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        var name = authentication.getName();
        return name == null || "anonymousUser".equals(name) ? null : name;
    }
}
