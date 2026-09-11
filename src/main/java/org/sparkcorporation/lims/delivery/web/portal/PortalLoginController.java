package org.sparkcorporation.lims.delivery.web.portal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Operation(
            summary = "Log in to the patient portal",
            description =
                    "Authenticates a patient with their portal login and password and, on success,"
                        + " mints a short-lived (30-minute) RS256 token for the patient-facing"
                        + " portal endpoints. This is a public, unauthenticated endpoint distinct"
                        + " from staff login; returns 401 for both an unknown login and a bad"
                        + " password, to avoid account enumeration.")
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

    @Schema(description = "Patient portal login credentials.")
    public record PortalLoginRequest(
            @Schema(description = "The patient's portal login identifier (not a staff username).")
                    @NotBlank
                    String portalLogin,
            @Schema(description = "The patient's portal password.") @NotBlank String password) {}

    @Schema(description = "A minted patient-portal session token.")
    public record PortalLoginResponse(
            @Schema(
                            description =
                                    "Short-lived RS256 bearer token to use on subsequent"
                                            + " /api/v1/portal/** requests.")
                    String token,
            @Schema(description = "Number of seconds until the token expires (30 minutes).")
                    long expiresInSeconds) {}
}
