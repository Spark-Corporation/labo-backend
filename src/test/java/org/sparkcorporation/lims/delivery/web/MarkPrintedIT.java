package org.sparkcorporation.lims.delivery.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

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
 * Acceptance IT for the {@code delivery} print-confirmation use-case (SP7 chunk 4). Exercises
 * {@code POST /api/v1/lab-files/{id}:mark-printed}: a PUBLISHED dossier yields 204, stamps {@code
 * lab_files.printed_at}, and records a {@code dossier.print} row in {@code signed_actions}; a
 * re-print overwrites the stamp; a non-PUBLISHED dossier is 409; an unknown dossier is 404; a
 * caller holding {@code lab-file.read} but not {@code dossier.print} (TECHNICIAN) is 403;
 * RECEPTIONIST (who holds the new grant) succeeds.
 *
 * <p>Cross-module fixtures are seeded via raw SQL (Modulith boundary); the dossier-driving flow
 * mirrors {@link PublishIT} / {@link ResultSendIT}.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MarkPrintedIT.Events.class)
class MarkPrintedIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ResultApi resultApi;
    @Autowired private Events events;

    private RestClient admin;
    private RestClient technician;
    private RestClient biologist;
    private RestClient receptionist;

    private String analysisCode;

    @BeforeEach
    void setUp() {
        admin = restClientFor(port, "ADMIN");
        technician = restClientFor(port, "TECHNICIAN");
        biologist = restClientFor(port, "BIOLOGIST");
        receptionist = restClientFor(port, "RECEPTIONIST");
        events.clear();
        var disciplineId = seedDiscipline();
        analysisCode = "GLU_" + Ulid.generate().substring(10, 16);
        seedAnalysis(analysisCode, disciplineId);
    }

    @Test
    void markPrinted_published_returns204_andStampsPrintedAt() {
        String patientId = seedPatient();
        String labFileId = aPublishedDossier(patientId);

        var resp =
                receptionist
                        .post()
                        .uri("/api/v1/lab-files/{id}:mark-printed", labFileId)
                        .retrieve()
                        .toBodilessEntity();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(printedAtOf(labFileId)).isNotNull();
        assertThat(signedPrintActions(labFileId)).isEqualTo(1L);
    }

    @Test
    void markPrinted_reprint_overwritesStamp() {
        String patientId = seedPatient();
        String labFileId = aPublishedDossier(patientId);

        receptionist
                .post()
                .uri("/api/v1/lab-files/{id}:mark-printed", labFileId)
                .retrieve()
                .toBodilessEntity();
        Instant first = printedAtOf(labFileId);

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            receptionist
                                    .post()
                                    .uri("/api/v1/lab-files/{id}:mark-printed", labFileId)
                                    .retrieve()
                                    .toBodilessEntity();
                            assertThat(printedAtOf(labFileId)).isAfterOrEqualTo(first);
                        });
    }

    @Test
    void markPrinted_notPublished_returns409() {
        String patientId = seedPatient();
        String labFileId = aFullyBioValidatedDossier(patientId);

        assertThatThrownBy(
                        () ->
                                receptionist
                                        .post()
                                        .uri("/api/v1/lab-files/{id}:mark-printed", labFileId)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(409)));
    }

    @Test
    void markPrinted_unknownDossier_returns404() {
        String unknownId = Ulid.generate();

        assertThatThrownBy(
                        () ->
                                receptionist
                                        .post()
                                        .uri("/api/v1/lab-files/{id}:mark-printed", unknownId)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(404)));
    }

    @Test
    void markPrinted_withoutPermission_returns403() {
        String patientId = seedPatient();
        String labFileId = aPublishedDossier(patientId);

        assertThatThrownBy(
                        () ->
                                technician
                                        .post()
                                        .uri("/api/v1/lab-files/{id}:mark-printed", labFileId)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(403)));
    }

    // ----- dossier-driving flow (mirrors PublishIT / ResultSendIT) --------------------------

    private String aPublishedDossier(String patientId) {
        String labFileId = aFullyBioValidatedDossier(patientId);
        biologist
                .post()
                .uri("/api/v1/lab-files/{id}:sign", labFileId)
                .retrieve()
                .toBodilessEntity();
        biologist
                .post()
                .uri("/api/v1/lab-files/{id}:publish", labFileId)
                .retrieve()
                .toBodilessEntity();
        return labFileId;
    }

    private String aFullyBioValidatedDossier(String patientId) {
        String labFileId = aDossierWithUnvalidatedResults(patientId);
        String resultId = resultIdFor(labFileId);
        biologist
                .post()
                .uri("/api/v1/results/" + resultId + ":validate-biological")
                .retrieve()
                .toBodilessEntity();
        return labFileId;
    }

    private String aDossierWithUnvalidatedResults(String patientId) {
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
        var resultId = resultIdFor(labFileId);
        var lines = resultApi.findActiveByLabFile(new LabFileId(labFileId));
        var lineId = lines.get(0).lines().get(0).id().value();

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
        return labFileId;
    }

    private String resultIdFor(String labFileId) {
        return events.resultCreated.stream()
                .filter(e -> e.labFileId().value().equals(labFileId))
                .findFirst()
                .orElseThrow()
                .resultId()
                .value();
    }

    private @org.jspecify.annotations.Nullable Instant printedAtOf(String labFileId) {
        return jdbc.queryForObject(
                "SELECT printed_at FROM lab_files WHERE id = ?", Instant.class, labFileId);
    }

    private Long signedPrintActions(String labFileId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM signed_actions WHERE action = 'dossier.print' AND resource ="
                        + " ?",
                Long.class,
                labFileId);
    }

    /** Spring-bean event recorder. */
    @TestConfiguration
    static class Events {
        final List<ResultCreated> resultCreated = new CopyOnWriteArrayList<>();

        @EventListener
        void onCreated(ResultCreated e) {
            resultCreated.add(e);
        }

        void clear() {
            resultCreated.clear();
        }
    }

    // ----- raw-SQL seeds --------------------------------------------------------------------

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
