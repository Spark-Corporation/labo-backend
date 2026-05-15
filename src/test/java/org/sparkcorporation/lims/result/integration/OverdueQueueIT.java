package org.sparkcorporation.lims.result.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Full HTTP + Testcontainers acceptance IT for {@code GET /api/v1/lab-files/queues/overdue} per CDC
 * "Dossiers en retard" (sub-project 1.4.c). Seeds one lab file with one overdue analysis and
 * verifies the response shape (DossierRow + OverdueInfo composition) end-to-end.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OverdueQueueIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;

    private RestClient http;
    private String labFileId;

    @BeforeEach
    void setUp() {
        http = restClientFor(port, "BIOLOGIST");
        seedFixture();
    }

    @Test
    void get_overdue_returns_lab_files_with_diagnostics() {
        ResponseEntity<Map<String, Object>> response =
                http.get()
                        .uri("/api/v1/lab-files/queues/overdue?page=0&size=200")
                        .retrieve()
                        .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = response.getBody();
        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        var content = (List<Map<?, ?>>) body.get("content");
        var row =
                content.stream()
                        .filter(
                                r ->
                                        labFileId.equals(
                                                ((Map<?, ?>) r.get("dossier")).get("labFileId")))
                        .findFirst();
        assertThat(row).isPresent();
        var dossier = (Map<?, ?>) row.get().get("dossier");
        var overdue = (Map<?, ?>) row.get().get("overdue");
        assertThat(dossier.get("labFileId")).isEqualTo(labFileId);
        assertThat((String) overdue.get("analysisCode")).startsWith("OD");
        assertThat(((Number) overdue.get("hoursPastDeadline")).intValue())
                .isGreaterThanOrEqualTo(2);
        assertThat(overdue.get("deadline")).isNotNull();
        assertThat(((Number) overdue.get("overdueAnalysisCount")).intValue()).isEqualTo(1);
    }

    private void seedFixture() {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        var analysisId = seedAnalysisWithDelay("OD", "Overdue", disciplineId, 0, 1);
        labFileId = seedLabFile(patientId, Instant.now().minus(3, ChronoUnit.HOURS));
        seedAnalysisRequest(labFileId, analysisId, "OD");
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

    private String seedAnalysisWithDelay(
            String code, String name, String disciplineId, int delayDays, int delayHours) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO analyses (id, code, name, kind, indiciary_value_b, label_count,"
                        + " discipline_id, result_type, delay_days, delay_hours, version) VALUES"
                        + " (?, ?, ?, 'SIMPLE', 10.00, 1, ?, 'REAL', ?, ?, 0)",
                id,
                code + "-" + id.substring(10, 16),
                name,
                disciplineId,
                delayDays,
                delayHours);
        return id;
    }

    private String seedLabFile(String patientId, Instant createdAt) {
        var id = Ulid.generate();
        var seq = String.format("%04d", REG_SEQ.getAndIncrement());
        var rn = "LF-" + LocalDate.now().format(DATE_FMT) + "-" + seq;
        jdbc.update(
                "INSERT INTO lab_files (id, registration_number, patient_id, urgent, status,"
                        + " created_at, updated_at, version) VALUES (?, ?, ?, false, 'OPEN', ?, ?,"
                        + " 0)",
                id,
                rn,
                patientId,
                java.sql.Timestamp.from(createdAt),
                java.sql.Timestamp.from(createdAt));
        return id;
    }

    private void seedAnalysisRequest(String labFileId, String analysisId, String code) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO analysis_requests (id, lab_file_id, analysis_id, analysis_code,"
                        + " requested_at, version) VALUES (?, ?, ?, (SELECT code FROM analyses"
                        + " WHERE id = ?), now(), 0)",
                id,
                labFileId,
                analysisId,
                analysisId);
    }
}
