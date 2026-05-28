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
import org.sparkcorporation.lims.delivery.api.DossierSigned;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.api.ResultApi;
import org.sparkcorporation.lims.result.api.ResultCreated;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Acceptance IT for the {@code delivery} electronic-signature use-case (SP3 Task 10). Exercises
 * {@code POST /api/v1/lab-files/{id}:sign}: a fully bio-validated dossier signs to {@code SIGNED}
 * and writes a digest-bearing {@code signed_actions} row + emits {@link DossierSigned}; a
 * partially-validated dossier is 409; a caller without {@code dossier.sign-electronically} is 403;
 * a double sign is idempotent.
 *
 * <p>Cross-module fixtures (patient + discipline + analysis) are seeded via raw SQL — the {@code
 * delivery} module cannot import other modules' entities (Modulith boundary). The dossier-driving
 * flow (open → enter line → validate-technical → validate-biological) mirrors {@link PublishIT}.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(SignDossierIT.Events.class)
class SignDossierIT extends IntegrationTestBase {

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
    void sign_succeeds_writes_digest_bearing_signed_action_and_emits_event() {
        String labFileId = aFullyBioValidatedDossier();

        var resp =
                biologist
                        .post()
                        .uri("/api/v1/lab-files/{id}:sign", labFileId)
                        .retrieve()
                        .toBodilessEntity();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statusOf(labFileId)).isEqualTo("SIGNED");

        // The explicit signing call records a signed_actions row carrying the content digest.
        Long signedActionRows =
                jdbc.queryForObject(
                        "SELECT count(*) FROM signed_actions WHERE action ="
                                + " 'dossier.sign-electronically' AND resource = ? AND"
                                + " metadata->>'contentSha256' IS NOT NULL",
                        Long.class,
                        labFileId);
        assertThat(signedActionRows).isEqualTo(1L);

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () ->
                                assertThat(events.signed)
                                        .anyMatch(
                                                e ->
                                                        e.labFileId().value().equals(labFileId)
                                                                && e.contentSha256() != null
                                                                && !e.contentSha256().isBlank()));
    }

    @Test
    void sign_returns_409_when_not_all_bio_validated() {
        String labFileId = aDossierWithUnvalidatedResults();

        assertThatThrownBy(
                        () ->
                                biologist
                                        .post()
                                        .uri("/api/v1/lab-files/{id}:sign", labFileId)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(409)));
    }

    @Test
    void sign_returns_403_without_sign_permission() {
        String labFileId = aFullyBioValidatedDossier();
        var receptionist = restClientFor(port, "RECEPTIONIST");

        assertThatThrownBy(
                        () ->
                                receptionist
                                        .post()
                                        .uri("/api/v1/lab-files/{id}:sign", labFileId)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(403)));
    }

    @Test
    void double_sign_is_idempotent_200() {
        String labFileId = aFullyBioValidatedDossier();
        biologist
                .post()
                .uri("/api/v1/lab-files/{id}:sign", labFileId)
                .retrieve()
                .toBodilessEntity();

        var second =
                biologist
                        .post()
                        .uri("/api/v1/lab-files/{id}:sign", labFileId)
                        .retrieve()
                        .toBodilessEntity();

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Idempotent: the second call does not write a second digest-bearing signing row.
        Long signedActionRows =
                jdbc.queryForObject(
                        "SELECT count(*) FROM signed_actions WHERE action ="
                                + " 'dossier.sign-electronically' AND resource = ?",
                        Long.class,
                        labFileId);
        assertThat(signedActionRows).isEqualTo(1L);
    }

    private String statusOf(String labFileId) {
        return jdbc.queryForObject(
                "SELECT status FROM lab_files WHERE id = ?", String.class, labFileId);
    }

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

    /** Spring-bean event recorder. */
    @TestConfiguration
    static class Events {
        final List<DossierSigned> signed = new CopyOnWriteArrayList<>();
        final List<ResultCreated> resultCreated = new CopyOnWriteArrayList<>();

        @EventListener
        void onSigned(DossierSigned e) {
            signed.add(e);
        }

        @EventListener
        void onCreated(ResultCreated e) {
            resultCreated.add(e);
        }

        void clear() {
            signed.clear();
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
