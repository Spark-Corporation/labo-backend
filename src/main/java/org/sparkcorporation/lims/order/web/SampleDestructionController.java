package org.sparkcorporation.lims.order.web;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.order.api.SampleId;
import org.sparkcorporation.lims.order.infra.MarkSampleDestroyedService;

/**
 * Records physical disposal of a sample (CDC Onglet « Conservation »). The colon-action is rendered
 * at the method level (mirrors {@link SampleAdminController}) so Spring's matcher does not treat
 * the trailing {@code :name} fragment as a path-segment continuation. Returns 204 No Content; the
 * call is idempotent (a second call on an already-destroyed sample is a no-op 204).
 */
@RestController
@RequestMapping("/api/v1/samples")
@RequiredArgsConstructor
public class SampleDestructionController {

    private final MarkSampleDestroyedService service;

    @PostMapping("/{id}:mark-destroyed")
    @Operation(
            summary = "Mark a sample as physically destroyed",
            description =
                    "Records the physical disposal of a sample once its retention period (CDC"
                            + " Onglet « Conservation ») has elapsed. The action is audit-signed."
                            + " Idempotent: a second call on an already-destroyed sample is a no-op"
                            + " 204.")
    @RequiresPermission("sample.destroy")
    public ResponseEntity<Void> markDestroyed(
            @PathVariable String id, @Nullable Authentication authentication) {
        service.markDestroyed(new SampleId(id), currentUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    private static @Nullable String currentUserId(@Nullable Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        var name = authentication.getName();
        return name == null || "anonymousUser".equals(name) ? null : name;
    }
}
