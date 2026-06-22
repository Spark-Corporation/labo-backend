package org.sparkcorporation.lims.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Acceptance IT for {@code POST /api/v1/samples/{id}:mark-destroyed} (CDC Onglet « Conservation »).
 * Boots the full Spring context against Testcontainers PostgreSQL, seeds cross-module fixtures,
 * records an in-lab sample, then exercises the disposal endpoint: 204 + idempotent re-call, 404 for
 * an unknown sample, 403 for a role lacking {@code sample.destroy} (RECEPTIONIST).
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SampleDestructionControllerIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;

    private RestClient admin;
    private String patientId;
    private String analysisId;
    private String analysisCode;
    private String recipientId;

    @BeforeEach
    void setUp() {
        admin = restClientFor(port, "ADMIN");
        patientId = seedPatient();
        var disciplineId = seedDiscipline("BIOCHIMIE");
        analysisCode = "GLU_" + Ulid.generate().substring(10, 16);
        analysisId = seedAnalysis(analysisCode, disciplineId);
        recipientId = seedRecipient();
    }

    @Test
    void mark_destroyed_returns_204_and_is_idempotent() {
        var sampleId = recordInLabSample();
        var technician = restClientFor(port, "TECHNICIAN");

        var first =
                technician
                        .post()
                        .uri("/api/v1/samples/" + sampleId + ":mark-destroyed")
                        .retrieve()
                        .toBodilessEntity();
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var second =
                technician
                        .post()
                        .uri("/api/v1/samples/" + sampleId + ":mark-destroyed")
                        .retrieve()
                        .toBodilessEntity();
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Instant destroyedAt =
                jdbc.queryForObject(
                        "SELECT destroyed_at FROM samples WHERE id = ?", Instant.class, sampleId);
        assertThat(destroyedAt).isNotNull();
    }

    @Test
    void unknown_sample_returns_404() {
        var unknownId = Ulid.generate();
        try {
            admin.post()
                    .uri("/api/v1/samples/" + unknownId + ":mark-destroyed")
                    .retrieve()
                    .toBodilessEntity();
            throw new AssertionError("expected 404 NOT_FOUND");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    void role_without_permission_is_forbidden() {
        var sampleId = recordInLabSample();
        var receptionist = restClientFor(port, "RECEPTIONIST");
        try {
            receptionist
                    .post()
                    .uri("/api/v1/samples/" + sampleId + ":mark-destroyed")
                    .retrieve()
                    .toBodilessEntity();
            throw new AssertionError("expected 403 FORBIDDEN");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    private String recordInLabSample() {
        var labFileId = openLabFile();
        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                admin.post()
                        .uri("/api/v1/lab-files/" + labFileId + "/samples:in-lab")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "analysisId",
                                        analysisId,
                                        "recipientId",
                                        recipientId,
                                        "collectedAt",
                                        Instant.now().toString()))
                        .retrieve()
                        .body(Map.class);
        return (String) body.get("id");
    }

    private String openLabFile() {
        @SuppressWarnings("unchecked")
        Map<String, Object> created =
                admin.post()
                        .uri("/api/v1/lab-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "patientId",
                                        patientId,
                                        "urgent",
                                        false,
                                        "analyses",
                                        List.of(analysisCode)))
                        .retrieve()
                        .body(Map.class);
        return (String) created.get("id");
    }

    private String seedPatient() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO patients (id, lab_code, last_name, civility, sex, insured, version)"
                        + " VALUES (?, ?, 'DOE', 'MADAME', 'F', false, 0)",
                id,
                "LAB-" + id.substring(0, 10));
        return id;
    }

    private String seedDiscipline(String prefix) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO disciplines (id, code, name, discipline_prefix, version) VALUES (?,"
                        + " ?, 'Biochimie', ?, 0)",
                id,
                "BIOCHEM-" + id.substring(10, 16),
                prefix);
        return id;
    }

    private String seedAnalysis(String code, String disciplineId) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO analyses (id, code, name, kind, indiciary_value_b, label_count,"
                        + " discipline_id, result_type, version) VALUES (?, ?, 'Glucose', 'SIMPLE',"
                        + " 10.00, 1, ?, 'REAL', 0)",
                id,
                code,
                disciplineId);
        return id;
    }

    private String seedRecipient() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO recipients (id, code, name, version) VALUES (?, ?, 'Tube EDTA', 0)",
                id,
                "TUBE-" + id.substring(10, 16));
        return id;
    }
}
