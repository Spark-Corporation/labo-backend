package org.sparkcorporation.lims.delivery.web.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.patient.web.CreatePatientRequest;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;

/**
 * Acceptance IT for the patient-portal login endpoint ({@code POST /api/v1/portal/login}) — the SP2
 * named first-failing test. A valid portal credential returns a 30-minute RS256 token; a wrong
 * password or unknown login is 401 (indistinguishable, to avoid account enumeration).
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PortalLoginIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Test
    void login_returns_token_for_valid_portal_credentials() {
        registerPatientWithPortal("alice-login", "s3cret-pass");
        var anon = restClientAnonymous(port);

        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                anon.post()
                        .uri("/api/v1/portal/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("portalLogin", "alice-login", "password", "s3cret-pass"))
                        .retrieve()
                        .body(Map.class);

        assertThat(body).isNotNull();
        assertThat((String) body.get("token")).isNotBlank();
        assertThat(((Number) body.get("expiresInSeconds")).longValue()).isEqualTo(1800L);
    }

    @Test
    void login_returns_401_for_wrong_password() {
        registerPatientWithPortal("bob-login", "right-pass");
        var anon = restClientAnonymous(port);
        assertThatThrownBy(
                        () ->
                                anon.post()
                                        .uri("/api/v1/portal/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(
                                                Map.of(
                                                        "portalLogin",
                                                        "bob-login",
                                                        "password",
                                                        "wrong-pass"))
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(401)));
    }

    @Test
    void login_returns_401_for_unknown_login() {
        var anon = restClientAnonymous(port);
        assertThatThrownBy(
                        () ->
                                anon.post()
                                        .uri("/api/v1/portal/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(
                                                Map.of(
                                                        "portalLogin",
                                                        "ghost",
                                                        "password",
                                                        "whatever"))
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(401)));
    }

    /**
     * Registers a patient with the given portal credentials via {@code POST /api/v1/patients} (as
     * ADMIN). {@code PatientService.create} hashes {@code portalPassword}; the raw values are then
     * usable against {@code /api/v1/portal/login}.
     */
    private void registerPatientWithPortal(String login, String password) {
        RestClient admin = restClientFor(port, "ADMIN");
        CreatePatientRequest req =
                new CreatePatientRequest(
                        new CreatePatientRequest.NameRequest("DOE", "Jane", null),
                        Civility.MADAME,
                        Sex.F,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new CreatePatientRequest.ContactRequest(null, null),
                        null,
                        null,
                        null,
                        login,
                        password);
        admin.post()
                .uri("/api/v1/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .toBodilessEntity();
    }
}
