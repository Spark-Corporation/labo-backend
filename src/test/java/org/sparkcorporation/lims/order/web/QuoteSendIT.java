package org.sparkcorporation.lims.order.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.notification.infra.NotificationRetryPoller;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * End-to-end IT for the quote {@code :send} trigger (spec §4.2). Seeds a patient (with email +
 * phone), a discipline, and an analysis via JDBC; creates and locks a quote via REST; sends it over
 * EMAIL + WHATSAPP; runs the poller and asserts both outbox rows reach {@code SENT}. Then covers
 * the three error paths: DRAFT → 409, missing email → 422, missing permission → 403.
 *
 * <p>Uses ADMIN for create/lock (which need {@code quote.create}/{@code quote.lock} plus {@code
 * quote.send}); TECHNICIAN lacks {@code quote.send} and is used for the 403 path.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuoteSendIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private NotificationRetryPoller poller;

    private RestClient admin;

    @BeforeEach
    void setUp() {
        admin = restClientFor(port, "ADMIN");
    }

    @Test
    void send_overEmailAndWhatsApp_enqueuesTwoEntries_thenPollerMarksThemSent() {
        String patientId = seedPatient("jane@example.test", "+22890000000");
        String quoteId = lockedQuote(patientId);

        var resp =
                admin.post()
                        .uri("/api/v1/quotes/{id}:send", quoteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("channels", List.of("EMAIL", "WHATSAPP")))
                        .retrieve()
                        .toEntity(Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(202));
        assertThat(pendingCount(quoteId)).isEqualTo(2);

        poller.poll();

        assertThat(sentCount(quoteId)).isEqualTo(2);
    }

    @Test
    void send_onDraftQuote_returns409() {
        String patientId = seedPatient("jane@example.test", "+22890000000");
        String quoteId = createDraftQuote(patientId);

        assertThatThrownBy(
                        () ->
                                admin.post()
                                        .uri("/api/v1/quotes/{id}:send", quoteId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(Map.of("channels", List.of("EMAIL")))
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(409)));
    }

    @Test
    void send_emailRequested_butPatientHasNoEmail_returns422() {
        String patientId = seedPatient(null, "+22890000000");
        String quoteId = lockedQuote(patientId);

        assertThatThrownBy(
                        () ->
                                admin.post()
                                        .uri("/api/v1/quotes/{id}:send", quoteId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(Map.of("channels", List.of("EMAIL")))
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(422)));
    }

    @Test
    void send_withoutQuoteSendPermission_returns403() {
        String patientId = seedPatient("jane@example.test", "+22890000000");
        String quoteId = lockedQuote(patientId);
        var technician = restClientFor(port, "TECHNICIAN");

        assertThatThrownBy(
                        () ->
                                technician
                                        .post()
                                        .uri("/api/v1/quotes/{id}:send", quoteId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(Map.of("channels", List.of("EMAIL")))
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(403)));
    }

    // ----- helpers --------------------------------------------------------------------------

    private int pendingCount(String quoteId) {
        Integer n =
                jdbc.queryForObject(
                        "SELECT count(*) FROM notification_outbox WHERE context_id = ? AND status"
                                + " = 'PENDING'",
                        Integer.class,
                        quoteId);
        return n == null ? 0 : n;
    }

    private int sentCount(String quoteId) {
        Integer n =
                jdbc.queryForObject(
                        "SELECT count(*) FROM notification_outbox WHERE context_id = ? AND status"
                                + " = 'SENT'",
                        Integer.class,
                        quoteId);
        return n == null ? 0 : n;
    }

    private String lockedQuote(String patientId) {
        String quoteId = createDraftQuote(patientId);
        admin.post().uri("/api/v1/quotes/" + quoteId + ":lock").retrieve().toBodilessEntity();
        return quoteId;
    }

    private String createDraftQuote(String patientId) {
        String analysisCode = seedAnalysis();
        var resp =
                admin.post()
                        .uri("/api/v1/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("patientId", patientId, "analyses", List.of(analysisCode)))
                        .retrieve()
                        .toEntity(Map.class);
        var location = resp.getHeaders().getLocation();
        if (location == null) {
            throw new AssertionError("expected Location header on POST /api/v1/quotes");
        }
        var path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private String seedPatient(@org.jspecify.annotations.Nullable String email, String phone) {
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

    private String seedAnalysis() {
        var disciplineId = Ulid.generate();
        jdbc.update(
                "INSERT INTO disciplines (id, code, name, discipline_prefix, version) VALUES (?,"
                        + " ?, 'Biochimie', 'BIOCHIMIE', 0)",
                disciplineId,
                "BIOCHEM-" + disciplineId.substring(10, 16));
        var analysisId = Ulid.generate();
        var code = "GLU_" + analysisId.substring(10, 16);
        jdbc.update(
                "INSERT INTO analyses (id, code, name, kind, indiciary_value_b, label_count,"
                        + " discipline_id, result_type, version) VALUES (?, ?, 'Glucose', 'SIMPLE',"
                        + " 10.00, 1, ?, 'REAL', 0)",
                analysisId,
                code,
                disciplineId);
        return code;
    }
}
