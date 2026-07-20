package org.sparkcorporation.lims.catalog.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PrescriberCrudIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void resetCatalogPractitionerState() {
        // The singleton Testcontainer is shared across IT classes, so prior ITs
        // (e.g. CatalogCodeCounterIT) leave the prescriber counter advanced and the
        // specialties table populated. Reset both so absolute-code assertions
        // (PRS-000001) and specialty-code uniqueness (CARDIOLOGY) hold here.
        jdbc.update("DELETE FROM preleveur_habilitations");
        jdbc.update("DELETE FROM preleveurs");
        jdbc.update("DELETE FROM prescribers");
        jdbc.update("DELETE FROM specialties");
        jdbc.update("UPDATE catalog_code_counters SET next_sequence = 1");
    }

    @Test
    void create_prescriber_with_specialty_then_get_returns_full_summary_with_specialty_snapshot() {
        String jwt = loginAs(port, "admin@test.local", "test-password");
        var http =
                RestClient.builder()
                        .baseUrl("http://localhost:" + port)
                        .defaultHeader("Authorization", "Bearer " + jwt)
                        .build();

        // 1. POST /api/v1/catalog/specialties
        Map<?, ?> spec =
                http.post()
                        .uri("/api/v1/catalog/specialties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("code", "CARDIOLOGY", "name", "Cardiologie"))
                        .retrieve()
                        .body(Map.class);
        String specialtyId = (String) spec.get("id");

        // 2. POST /api/v1/catalog/prescribers
        Map<?, ?> pres =
                http.post()
                        .uri("/api/v1/catalog/prescribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "title", "Dr.",
                                        "name", "AGBETI Kossi",
                                        "specialtyId", specialtyId,
                                        "phone", "+228 90 00 00 00",
                                        "email", "k.agbeti@example.tg",
                                        "licenseNumber", "ONMT-12345",
                                        "institution", "Clinique Saint-Joseph"))
                        .retrieve()
                        .body(Map.class);
        String prescriberId = (String) pres.get("id");
        assertThat(pres.get("code")).isEqualTo("PRS-000001");

        // 3. GET /api/v1/catalog/prescribers/{id}
        Map<?, ?> got =
                http.get()
                        .uri("/api/v1/catalog/prescribers/{id}", prescriberId)
                        .retrieve()
                        .body(Map.class);
        assertThat(got.get("code")).isEqualTo("PRS-000001");
        assertThat(got.get("title")).isEqualTo("Dr.");
        assertThat(got.get("name")).isEqualTo("AGBETI Kossi");
        assertThat(got.get("licenseNumber")).isEqualTo("ONMT-12345");
        assertThat(got.get("institution")).isEqualTo("Clinique Saint-Joseph");

        Map<?, ?> snap = (Map<?, ?>) got.get("specialty");
        assertThat(snap.get("code")).isEqualTo("CARDIOLOGY");
        assertThat(snap.get("name")).isEqualTo("Cardiologie");
    }

    @Test
    void delete_prescriber_twice_returns_204_each_time() {
        String jwt = loginAs(port, "admin@test.local", "test-password");
        var http =
                RestClient.builder()
                        .baseUrl("http://localhost:" + port)
                        .defaultHeader("Authorization", "Bearer " + jwt)
                        .build();
        Map<?, ?> created =
                http.post()
                        .uri("/api/v1/catalog/prescribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("name", "TO_DELETE"))
                        .retrieve()
                        .body(Map.class);
        String id = (String) created.get("id");

        HttpStatusCode first =
                http.delete()
                        .uri("/api/v1/catalog/prescribers/{id}", id)
                        .exchange((req, res) -> res.getStatusCode());
        assertThat(first).isEqualTo(HttpStatus.NO_CONTENT);

        HttpStatusCode second =
                http.delete()
                        .uri("/api/v1/catalog/prescribers/{id}", id)
                        .exchange((req, res) -> res.getStatusCode());
        assertThat(second).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
