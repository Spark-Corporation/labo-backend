package org.sparkcorporation.lims.order.web;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

import org.sparkcorporation.lims.order.api.DiscountKind;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.domain.ImmutablePatientException;
import org.sparkcorporation.lims.order.domain.ImmutableRegistrationNumberException;
import org.sparkcorporation.lims.order.domain.ImmutableStatusException;
import org.sparkcorporation.lims.order.domain.UpdateLabFile;

/**
 * Admin REST controller for {@link org.sparkcorporation.lims.order.domain.LabFile} per spec §7.1.
 *
 * <p>PATCH semantics (spec §5.4): the body binds as a {@link Map} so the immutable keys ({@code
 * registrationNumber}, {@code patientId}, {@code status}) can be detected and rejected with 422
 * before the whitelisted keys ({@code urgent}, {@code prescriberId}, {@code preleveurId}) flow onto
 * {@link UpdateLabFile}. Bean-Validation on PATCH is a known follow-up across all admin
 * controllers.
 */
@RestController
@RequestMapping("/api/v1/lab-files")
@RequiredArgsConstructor
public class LabFileAdminController {

    private final LabFileService service;

    @PostMapping
    @RequiresPermission("lab-file.create")
    public ResponseEntity<LabFileResponse> create(
            @Valid @RequestBody CreateLabFileRequest req, @Nullable Authentication authentication) {
        var resp = service.create(req, currentUserId(authentication));
        return ResponseEntity.created(URI.create("/api/v1/lab-files/" + resp.id())).body(resp);
    }

    @GetMapping
    @RequiresPermission("lab-file.read")
    public LabFileSearchResponse list(
            @RequestParam(required = false) @Nullable String patientId,
            @RequestParam(required = false) @Nullable String prescriberId,
            @RequestParam(required = false) @Nullable Boolean urgent,
            @RequestParam(required = false) @Nullable LabFileStatus status,
            @RequestParam(required = false) @Nullable Instant from,
            @RequestParam(required = false) @Nullable Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.search(patientId, prescriberId, urgent, status, from, to, page, size);
    }

    @GetMapping("/{id}")
    @RequiresPermission("lab-file.read")
    public LabFileResponse get(@PathVariable String id) {
        return service.get(new LabFileId(id));
    }

    @PatchMapping("/{id}")
    @RequiresPermission("lab-file.create")
    public LabFileResponse update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        if (body.containsKey("registrationNumber")) {
            throw new ImmutableRegistrationNumberException(
                    "registrationNumber is immutable; remove the 'registrationNumber' field from"
                            + " the PATCH body");
        }
        if (body.containsKey("patientId")) {
            throw new ImmutablePatientException(
                    "patientId is immutable; remove the 'patientId' field from the PATCH body");
        }
        if (body.containsKey("status")) {
            throw new ImmutableStatusException(
                    "status transitions only via state-machine endpoints (1.5); remove the"
                            + " 'status' field from the PATCH body");
        }
        var cmd =
                new UpdateLabFile(
                        asBoolean(body.get("urgent")),
                        asString(body.get("prescriberId")),
                        asString(body.get("preleveurId")),
                        asString(body.get("insuranceId")),
                        asString(body.get("partnerId")),
                        asBigDecimal(body.get("homeCollectionFee")),
                        asBigDecimal(body.get("additionalFees")),
                        asDiscountKind(body.get("discountKind")),
                        asBigDecimal(body.get("discountValue")));
        return service.update(new LabFileId(id), cmd);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("lab-file.create")
    public void delete(@PathVariable String id) {
        service.delete(new LabFileId(id));
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

    private static @Nullable Boolean asBoolean(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        throw new IllegalArgumentException(
                "expected boolean value, got " + value.getClass().getSimpleName());
    }

    private static @Nullable BigDecimal asBigDecimal(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        if (value instanceof String s) {
            return new BigDecimal(s);
        }
        throw new IllegalArgumentException(
                "expected numeric value, got " + value.getClass().getSimpleName());
    }

    private static @Nullable DiscountKind asDiscountKind(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof DiscountKind dk) {
            return dk;
        }
        if (value instanceof String s) {
            return DiscountKind.valueOf(s);
        }
        throw new IllegalArgumentException(
                "expected DiscountKind value, got " + value.getClass().getSimpleName());
    }

    /**
     * Extracts the current user's id (the principal name) from the security context, or {@code
     * null} when unauthenticated. Mirrors the IAM module's pattern (e.g. {@code SelfController}).
     */
    private static @Nullable String currentUserId(@Nullable Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        var name = authentication.getName();
        return name == null || "anonymousUser".equals(name) ? null : name;
    }
}
