package org.sparkcorporation.lims.delivery.infra;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;

import org.sparkcorporation.lims.patient.api.PatientId;

/**
 * Path-scoped gate for the patient-portal realm. Verifies the {@code Authorization: Bearer <token>}
 * header as an RS256 portal JWT (via {@link PortalTokenService}) and exposes the authenticated
 * patient id as a request attribute for the portal controllers. Anything missing/invalid is 401.
 *
 * <p>The login path is excluded ({@link #shouldNotFilter}) because it is the credential exchange
 * itself — it has no token yet. Registered via a {@code FilterRegistrationBean} (see {@link
 * DeliverySecurityConfig}), <b>not</b> as a {@code @Component}, so it only runs for {@code
 * /api/v1/portal/*} and never registers globally for {@code /*}.
 */
public class PortalAuthFilter extends OncePerRequestFilter {

    public static final String PATIENT_ID_ATTR = "portalPatientId";
    private static final String LOGIN_PATH = "/api/v1/portal/login";
    private static final String BEARER_PREFIX = "Bearer ";

    private final PortalTokenService tokenService;

    public PortalAuthFilter(PortalTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return LOGIN_PATH.equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        try {
            PatientId patientId = tokenService.verify(header.substring(BEARER_PREFIX.length()));
            request.setAttribute(PATIENT_ID_ATTR, patientId.value());
        } catch (JwtException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        chain.doFilter(request, response);
    }
}
