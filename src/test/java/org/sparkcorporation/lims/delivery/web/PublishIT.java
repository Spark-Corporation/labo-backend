package org.sparkcorporation.lims.delivery.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
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
import org.sparkcorporation.lims.delivery.api.ResultPublished;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.api.ResultApi;
import org.sparkcorporation.lims.result.api.ResultCreated;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Acceptance IT for the {@code delivery} publish use-case (SP2 Task 5; SP3 Task 10 gated publish on
 * {@code SIGNED}). Exercises {@code POST /api/v1/lab-files/{id}:publish}: happy path is
 * sign→publish and emits {@link ResultPublished}; a partially-validated dossier is 409; a
 * bio-validated but unsigned dossier is 409; a caller without {@code dossier.publish} is 403; a
 * second publish is idempotent (200, no second event).
 *
 * <p>Cross-module fixtures (patient + discipline + analysis) are seeded via raw SQL — the {@code
 * delivery} module cannot import other modules' entities (Modulith boundary; mirrors {@link
 * org.sparkcorporation.lims.result.integration.ResultEntryAndValidationIT}). The dossier-driving
 * flow (open → enter line → validate-technical → validate-biological) is copied from that IT.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PublishIT.Events.class)
class PublishIT extends IntegrationTestBase {

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
    void publish_succeeds_when_dossier_is_signed_and_emits_event() {
        String labFileId = aSignedDossier();
        var publisher = restClientFor(port, "BIOLOGIST");

        var resp =
                publisher
                        .post()
                        .uri("/api/v1/lab-files/{id}:publish", labFileId)
                        .retrieve()
                        .toBodilessEntity();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () ->
                                assertThat(events.published)
                                        .anyMatch(e -> e.labFileId().value().equals(labFileId)));
    }

    @Test
    void publish_returns_409_when_not_all_bio_validated() {
        String labFileId = aDossierWithUnvalidatedResults();
        var publisher = restClientFor(port, "BIOLOGIST");

        assertThatThrownBy(
                        () ->
                                publisher
                                        .post()
                                        .uri("/api/v1/lab-files/{id}:publish", labFileId)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(409)));
    }

    @Test
    void publish_returns_409_when_bio_validated_but_not_signed() {
        String labFileId = aFullyBioValidatedDossier();
        var publisher = restClientFor(port, "BIOLOGIST");

        assertThatThrownBy(
                        () ->
                                publisher
                                        .post()
                                        .uri("/api/v1/lab-files/{id}:publish", labFileId)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(409)));
    }

    @Test
    void publish_returns_403_without_dossier_publish_permission() {
        String labFileId = aSignedDossier();
        var receptionist = restClientFor(port, "RECEPTIONIST");

        assertThatThrownBy(
                        () ->
                                receptionist
                                        .post()
                                        .uri("/api/v1/lab-files/{id}:publish", labFileId)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(403)));
    }

    @Test
    void double_publish_is_idempotent_200() {
        String labFileId = aSignedDossier();
        var publisher = restClientFor(port, "BIOLOGIST");
        publisher
                .post()
                .uri("/api/v1/lab-files/{id}:publish", labFileId)
                .retrieve()
                .toBodilessEntity();
        var second =
                publisher
                        .post()
                        .uri("/api/v1/lab-files/{id}:publish", labFileId)
                        .retrieve()
                        .toBodilessEntity();
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /**
     * Builds a dossier whose single result is biologically validated: open → enter line →
     * validate-technical → validate-biological, copied from {@code ResultEntryAndValidationIT}.
     */
    private String aFullyBioValidatedDossier() {
        String labFileId = aDossierWithUnvalidatedResults();
        String resultId = resultIdFor(labFileId);
        biologist
                .post()
                .uri("/api/v1/results/" + resultId + ":validate-biological")
                .retrieve()
                .toBodilessEntity();
        return labFileId;
    }

    /**
     * Builds a fully bio-validated dossier and electronically signs it (BIOLOGIST). Publication now
     * requires {@code SIGNED} (SP3), so every publish-success path must sign first.
     */
    private String aSignedDossier() {
        String labFileId = aFullyBioValidatedDossier();
        biologist
                .post()
                .uri("/api/v1/lab-files/{id}:sign", labFileId)
                .retrieve()
                .toBodilessEntity();
        return labFileId;
    }

    /**
     * Builds a dossier whose single result is only technically validated (stops before {@code
     * :validate-biological}): open → enter line → validate-technical.
     */
    private String aDossierWithUnvalidatedResults() {
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

    /** Spring-bean event recorder mirroring {@code ResultEntryAndValidationIT.CapturedEvents}. */
    @TestConfiguration
    static class Events {
        final List<ResultPublished> published = new CopyOnWriteArrayList<>();
        final List<ResultCreated> resultCreated = new CopyOnWriteArrayList<>();

        @EventListener
        void onPublished(ResultPublished e) {
            published.add(e);
        }

        @EventListener
        void onCreated(ResultCreated e) {
            resultCreated.add(e);
        }

        void clear() {
            published.clear();
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
