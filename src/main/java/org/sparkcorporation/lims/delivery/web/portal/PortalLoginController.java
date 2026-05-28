package org.sparkcorporation.lims.delivery.web.portal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.delivery.infra.PortalTokenService;
import org.sparkcorporation.lims.patient.api.PatientPortalApi;

/**
 * Patient-portal login — {@code POST /api/v1/portal/login}. A public path (gated by the {@code
 * PortalAuthFilter} only for the protected portal endpoints, not for login). Returns a 30-minute
 * RS256 portal token on valid credentials, 401 otherwise (the failure is indistinguishable for an
 * unknown login vs. a bad password, to avoid account enumeration).
 */
@RestController
@RequestMapping("/api/v1/portal")
@RequiredArgsConstructor
public class PortalLoginController {

    private final PatientPortalApi patientPortalApi;
    private final PortalTokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<PortalLoginResponse> login(@Valid @RequestBody PortalLoginRequest req) {
        return patientPortalApi
                .authenticate(req.portalLogin(), req.password())
                .map(tokenService::mint)
                .map(
                        t ->
                                ResponseEntity.ok(
                                        new PortalLoginResponse(t.token(), t.expiresInSeconds())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    public record PortalLoginRequest(@NotBlank String portalLogin, @NotBlank String password) {}

    public record PortalLoginResponse(String token, long expiresInSeconds) {}
}
