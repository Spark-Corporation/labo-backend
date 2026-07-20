package org.sparkcorporation.lims.labsettings.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.labsettings.api.LabSettings;
import org.sparkcorporation.lims.labsettings.api.LabSettingsApi;
import org.sparkcorporation.lims.labsettings.api.LabSettingsUpdateRequest;

// Tests share the JVM-scoped Testcontainers PostgreSQL singleton; PUT_returns200_* mutates the
// singleton row. Lock method order so GET_returns200_* runs against the pristine Liquibase-seeded
// row before any PUT mutates it (mirrors JdbcLabSettingsApiIT chunk-2 pattern). @AfterEach also
// restores the seed values so subsequent IT classes inherit a known state.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.MethodName.class)
class LabSettingsControllerIT extends IntegrationTestBase {

    @LocalServerPort int port;

    @Autowired LabSettingsApi api;

    @AfterEach
    void restoreSeed() {
        api.update(
                new LabSettingsUpdateRequest(
                        "Laboratoire",
                        "Adresse à configurer",
                        "Numéro d'accréditation à configurer",
                        "Directeur à configurer"));
    }

    @Test
    void GET_returns200_forTechnician_whoHasLabFileRead() {
        var http = restClientFor(port, "TECHNICIAN");

        var body = http.get().uri("/api/v1/lab-settings").retrieve().body(LabSettings.class);

        assertThat(body).isNotNull();
        assertThat(body.name()).isNotBlank();
    }

    @Test
    void GET_returns403_forCashier_withoutReadPermission() {
        // CASHIER does not hold lab-file.read (per 202605061001-seed-iam-catalog.yaml),
        // so the chunk-1 grant did not propagate lab-settings.read to this role.
        var cashier = restClientFor(port, "CASHIER");

        assertThatThrownBy(
                        () ->
                                cashier.get()
                                        .uri("/api/v1/lab-settings")
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(403)));
    }

    @Test
    void PUT_returns200_forAdmin_andPersistsAcrossRequests() {
        var admin = restClientFor(port, "ADMIN");
        var req =
                new LabSettingsUpdateRequest(
                        "Laboratoire Sparkcorp",
                        "12 Avenue du Test, Lomé",
                        "ACC-2026-001",
                        "Dr. KOFFI Adjoa");

        var putBody =
                admin.put()
                        .uri("/api/v1/lab-settings")
                        .body(req)
                        .retrieve()
                        .body(LabSettings.class);

        assertThat(putBody).isNotNull();
        assertThat(putBody.name()).isEqualTo("Laboratoire Sparkcorp");

        var getBody = admin.get().uri("/api/v1/lab-settings").retrieve().body(LabSettings.class);

        assertThat(getBody.name()).isEqualTo("Laboratoire Sparkcorp");
    }

    @Test
    void PUT_returns403_forTechnician_withoutManagePermission() {
        var tech = restClientFor(port, "TECHNICIAN");
        var req = new LabSettingsUpdateRequest("n", "a", "ac", "m");

        assertThatThrownBy(
                        () ->
                                tech.put()
                                        .uri("/api/v1/lab-settings")
                                        .body(req)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(403)));
    }

    @Test
    void PUT_returns422_forAdmin_onBlankName() {
        var admin = restClientFor(port, "ADMIN");
        var req = new LabSettingsUpdateRequest("", "address", "accreditation", "manager");

        assertThatThrownBy(
                        () ->
                                admin.put()
                                        .uri("/api/v1/lab-settings")
                                        .body(req)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(422)));
    }

    @Test
    void PUT_returns422_forAdmin_onBlankAccreditation() {
        var admin = restClientFor(port, "ADMIN");
        var req = new LabSettingsUpdateRequest("name", "address", "", "manager");

        assertThatThrownBy(
                        () ->
                                admin.put()
                                        .uri("/api/v1/lab-settings")
                                        .body(req)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(422)));
    }
}
