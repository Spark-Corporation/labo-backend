package org.sparkcorporation.lims.result.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.api.ResultApi;
import org.sparkcorporation.lims.result.api.ResultCreated;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Acceptance IT for the {@code result} edit-lock (SP3 Task 11). Once a dossier is electronically
 * {@code SIGNED} (or {@code PUBLISHED}), every mutating result endpoint must be refused with a 409
 * {@code /errors/result/dossier.locked}. A mutation on an OPEN (unsigned) dossier still succeeds.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ResultEditLockIT.Events.class)
class ResultEditLockIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ResultApi resultApi;
    @Autowired private Events events;

    private RestClient admin;
    private RestClient technician;
    private RestClient biologist;

    private String patientId;
    private String analysisCode;

    @BeforeEach
    void setUp() {
        admin = restClientFor(port, "ADMIN");
        technician = restClientFor(port, "TECHNICIAN");
        biologist = restClientFor(port, "BIOLOGIST");
        events.clear();
        patientId = seedPatient();
        var disciplineId = seedDiscipline();
        analysisCode = "GLU_" + Ulid.generate().substring(10, 16);
        seedAnalysis(analysisCode, disciplineId);
    }

    @Test
    void patch_line_on_open_dossier_succeeds() {
        String labFileId = openDossierWithOneResult();
        String resultId = resultIdFor(labFileId);
        String lineId = firstLineId(labFileId);

        var resp =
                technician
                        .patch()
                        .uri("/api/v1/results/" + resultId + "/lines/" + lineId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("value", Map.of("kind", "REAL", "value", "1.234")))
                        .retrieve()
                        .toBodilessEntity();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void patch_line_on_signed_dossier_is_409_dossier_locked() {
        String labFileId = aSignedDossier();
        String resultId = resultIdFor(labFileId);
        String lineId = firstLineId(labFileId);

        assertThatThrownBy(
                        () ->
                                technician
                                        .patch()
                                        .uri("/api/v1/results/" + resultId + "/lines/" + lineId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(
                                                Map.of(
                                                        "value",
                                                        Map.of("kind", "REAL", "value", "9.999")))
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex -> {
                            assertThat(ex.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(409));
                            assertThat(ex.getResponseBodyAsString())
                                    .contains("/errors/result/dossier.locked");
                        });
    }

    private String aSignedDossier() {
        String labFileId = openDossierWithOneResult();
        String resultId = resultIdFor(labFileId);
        String lineId = firstLineId(labFileId);
        technician
                .patch()
                .uri("/api/v1/results/" + resultId + "/lines/" + lineId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("value", Map.of("kind", "REAL", "value", "1.234")))
                .retrieve()
                .toBodilessEntity();
        technician
                .post()
                .uri("/api/v1/results/" + resultId + ":validate-technical")
                .retrieve()
                .toBodilessEntity();
        biologist
                .post()
                .uri("/api/v1/results/" + resultId + ":validate-biological")
                .retrieve()
                .toBodilessEntity();
        biologist
                .post()
                .uri("/api/v1/lab-files/{id}:sign", labFileId)
                .retrieve()
                .toBodilessEntity();
        return labFileId;
    }

    private String openDossierWithOneResult() {
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> created =
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
                        .toEntity(Map.class);
        var labFileId = (String) created.getBody().get("id");

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () ->
                                assertThat(events.resultCreated)
                                        .anyMatch(e -> e.labFileId().value().equals(labFileId)));
        return labFileId;
    }

    private String firstLineId(String labFileId) {
        var lines = resultApi.findActiveByLabFile(new LabFileId(labFileId));
        return lines.get(0).lines().get(0).id().value();
    }

    private String resultIdFor(String labFileId) {
        return events.resultCreated.stream()
                .filter(e -> e.labFileId().value().equals(labFileId))
                .findFirst()
                .orElseThrow()
                .resultId()
                .value();
    }

    /** Spring-bean event recorder. */
    @TestConfiguration
    static class Events {
        final List<ResultCreated> resultCreated = new java.util.concurrent.CopyOnWriteArrayList<>();

        @EventListener
        void onCreated(ResultCreated e) {
            resultCreated.add(e);
        }

        void clear() {
            resultCreated.clear();
        }
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

    private String seedDiscipline() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO disciplines (id, code, name, discipline_prefix, version) VALUES (?,"
                        + " ?, 'Biochimie', 'BIOCHIMIE', 0)",
                id,
                "BIOCHEM-" + id.substring(10, 16));
        return id;
    }

    private void seedAnalysis(String code, String disciplineId) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO analyses (id, code, name, kind, indiciary_value_b, label_count,"
                        + " discipline_id, result_type, version) VALUES (?, ?, 'Glucose', 'SIMPLE',"
                        + " 10.00, 1, ?, 'REAL', 0)",
                id,
                code,
                disciplineId);
    }
}
