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
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.notification.infra.NotificationRetryPoller;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.api.ResultApi;
import org.sparkcorporation.lims.result.api.ResultCreated;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Acceptance IT for the {@code delivery} result-send use-case (SP4 chunk 5). Exercises {@code POST
 * /api/v1/lab-files/{id}:send}: a PUBLISHED dossier whose patient has email + phone yields 202 with
 * NO password in the body, two PENDING outbox rows (encrypted EMAIL + WhatsApp password), and after
 * {@code poller.poll()} both rows reach SENT, {@code lab_files.emailed_at} is stamped, and a {@code
 * dossier.send} row appears in {@code signed_actions}. Error paths: non-PUBLISHED → 409, no email →
 * 422, no phone → password present in body + EMAIL row only, missing {@code dossier.send} → 403.
 *
 * <p>Cross-module fixtures are seeded via raw SQL (Modulith boundary); the dossier-driving flow
 * mirrors {@link PublishIT} / {@link SignDossierIT}.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ResultSendIT.Events.class)
class ResultSendIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ResultApi resultApi;
    @Autowired private NotificationRetryPoller poller;
    @Autowired private Events events;

    private RestClient admin;
    private RestClient technician;
    private RestClient biologist;

    private String analysisCode;

    @BeforeEach
    void setUp() {
        admin = restClientFor(port, "ADMIN");
        technician = restClientFor(port, "TECHNICIAN");
        biologist = restClientFor(port, "BIOLOGIST");
        events.clear();
        var disciplineId = seedDiscipline();
        analysisCode = "GLU_" + Ulid.generate().substring(10, 16);
        seedAnalysis(analysisCode, disciplineId);
    }

    @Test
    void
            send_publishedDossier_withEmailAndPhone_enqueuesTwo_noPasswordInBody_thenPollerSentsAndStampsAndSigns() {
        String patientId = seedPatient("jane@example.test", "+22890000000");
        String labFileId = aPublishedDossier(patientId);

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> resp =
                biologist
                        .post()
                        .uri("/api/v1/lab-files/{id}:send", labFileId)
                        .retrieve()
                        .toEntity(Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(202));
        assertThat(resp.getBody().get("password")).isNull();
        assertThat(resp.getBody().get("emailNotificationId")).isNotNull();
        assertThat(resp.getBody().get("whatsAppNotificationId")).isNotNull();
        assertThat(pendingCount(labFileId)).isEqualTo(2);

        poller.poll();

        assertThat(sentCount(labFileId)).isEqualTo(2);
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(emailedAtOf(labFileId)).isNotNull());
        assertThat(signedSendActions(labFileId)).isEqualTo(1L);
    }

    @Test
    void send_nonPublishedDossier_returns409() {
        String patientId = seedPatient("jane@example.test", "+22890000000");
        String labFileId = aFullyBioValidatedDossier(patientId); // not signed, not published

        assertThatThrownBy(
                        () ->
                                biologist
                                        .post()
                                        .uri("/api/v1/lab-files/{id}:send", labFileId)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(409)));
    }

    @Test
    void send_publishedDossier_butPatientHasNoEmail_returns422() {
        String patientId = seedPatient(null, "+22890000000");
        String labFileId = aPublishedDossier(patientId);

        assertThatThrownBy(
                        () ->
                                biologist
                                        .post()
                                        .uri("/api/v1/lab-files/{id}:send", labFileId)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(422)));
    }

    @Test
    void send_publishedDossier_withNoPhone_returnsPasswordInBody_andOnlyEmailRow() {
        String patientId = seedPatient("jane@example.test", null);
        String labFileId = aPublishedDossier(patientId);

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> resp =
                biologist
                        .post()
                        .uri("/api/v1/lab-files/{id}:send", labFileId)
                        .retrieve()
                        .toEntity(Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(202));
        assertThat(resp.getBody().get("password")).isNotNull();
        assertThat(resp.getBody().get("whatsAppNotificationId")).isNull();
        assertThat(pendingCount(labFileId)).isEqualTo(1);
    }

    @Test
    void send_withoutDossierSendPermission_returns403() {
        String patientId = seedPatient("jane@example.test", "+22890000000");
        String labFileId = aPublishedDossier(patientId);
        var receptionist = restClientFor(port, "RECEPTIONIST");

        assertThatThrownBy(
                        () ->
                                receptionist
                                        .post()
                                        .uri("/api/v1/lab-files/{id}:send", labFileId)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(403)));
    }

    // ----- DB helpers -----------------------------------------------------------------------

    private int pendingCount(String labFileId) {
        Integer n =
                jdbc.queryForObject(
                        "SELECT count(*) FROM notification_outbox WHERE context_id = ? AND status ="
                                + " 'PENDING'",
                        Integer.class,
                        labFileId);
        return n == null ? 0 : n;
    }

    private int sentCount(String labFileId) {
        Integer n =
                jdbc.queryForObject(
                        "SELECT count(*) FROM notification_outbox WHERE context_id = ? AND status ="
                                + " 'SENT'",
                        Integer.class,
                        labFileId);
        return n == null ? 0 : n;
    }

    private @org.jspecify.annotations.Nullable Instant emailedAtOf(String labFileId) {
        return jdbc.queryForObject(
                "SELECT emailed_at FROM lab_files WHERE id = ?", Instant.class, labFileId);
    }

    private Long signedSendActions(String labFileId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM signed_actions WHERE action = 'dossier.send' AND resource ="
                        + " ?",
                Long.class,
                labFileId);
    }

    // ----- dossier-driving flow (mirrors PublishIT) -----------------------------------------

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

    private String seedPatient(
            @org.jspecify.annotations.Nullable String email,
            @org.jspecify.annotations.Nullable String phone) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO patients (id, lab_code, last_name, first_names, civility, sex,"
                        + " insured, phone, email, version) VALUES (?, ?, 'DOE', 'Jane', 'MADAME',"
                        + " 'F', false, ?, ?, 0)",
                id,
                "LAB-" + id.substring(0, 10),
                phone,
                email);
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
