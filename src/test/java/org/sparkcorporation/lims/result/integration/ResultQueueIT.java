package org.sparkcorporation.lims.result.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Full HTTP + Testcontainers acceptance IT for {@code GET /api/v1/results/queues/to-validate} and
 * {@code /to-print} per spec §X biologist queues. Seeds results in various states and asserts that
 * each endpoint returns only its target state.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResultQueueIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;

    private RestClient http;

    @BeforeEach
    void setUp() {
        http = restClientFor(port, "BIOLOGIST");
        seedMixedStateResults();
    }

    @Test
    void to_validate_returns_only_technical_validated_results() {
        @SuppressWarnings("rawtypes")
        var resp =
                http.get().uri("/api/v1/results/queues/to-validate").retrieve().toEntity(Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(((Number) body.get("totalElements")).intValue()).isGreaterThanOrEqualTo(1);
        @SuppressWarnings("unchecked")
        var content = (List<Map<?, ?>>) body.get("content");
        assertThat(content).isNotEmpty();
        content.forEach(row -> assertThat(row.get("state")).isEqualTo("TECHNICAL_VALIDATED"));
    }

    @Test
    void to_print_returns_only_biological_validated_results() {
        @SuppressWarnings("rawtypes")
        var resp = http.get().uri("/api/v1/results/queues/to-print").retrieve().toEntity(Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(((Number) body.get("totalElements")).intValue()).isGreaterThanOrEqualTo(1);
        @SuppressWarnings("unchecked")
        var content = (List<Map<?, ?>>) body.get("content");
        assertThat(content).isNotEmpty();
        content.forEach(row -> assertThat(row.get("state")).isEqualTo("BIOLOGICAL_VALIDATED"));
    }

    private void seedMixedStateResults() {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        // Each analysis_request must have a distinct (lab_file_id, analysis_id) — use 3 analyses
        var analysisIdDraft = seedAnalysis("GLU", disciplineId);
        var analysisIdTechV = seedAnalysis("CBC", disciplineId);
        var analysisIdBioV = seedAnalysis("CRP", disciplineId);
        var labFileId = seedLabFile(patientId);
        var arDraft = seedAnalysisRequest(labFileId, analysisIdDraft, "GLU");
        var arTechValidated = seedAnalysisRequest(labFileId, analysisIdTechV, "CBC");
        var arBioValidated = seedAnalysisRequest(labFileId, analysisIdBioV, "CRP");
        seedResult(labFileId, arDraft, analysisIdDraft, "GLU", "DRAFT");
        seedResult(labFileId, arTechValidated, analysisIdTechV, "CBC", "TECHNICAL_VALIDATED");
        seedResult(labFileId, arBioValidated, analysisIdBioV, "CRP", "BIOLOGICAL_VALIDATED");
    }

    private String seedPatient() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO patients (id, lab_code, last_name, civility, sex, insured, version)"
                        + " VALUES (?, ?, 'DOE', 'MADAME', 'F', false, 0)",
                id,
                "LAB-" + id.substring(0, 26));
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

    private String seedAnalysis(String code, String disciplineId) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO analyses (id, code, name, kind, indiciary_value_b, label_count,"
                        + " discipline_id, result_type, version) VALUES (?, ?, ?, 'SIMPLE',"
                        + " 10.00, 1, ?, 'REAL', 0)",
                id,
                code + "-" + id.substring(10, 16),
                code,
                disciplineId);
        return id;
    }

    private String seedLabFile(String patientId) {
        var id = Ulid.generate();
        var seq = String.format("%04d", REG_SEQ.getAndIncrement());
        var rn = "LF-" + LocalDate.now().format(DATE_FMT) + "-" + seq;
        jdbc.update(
                "INSERT INTO lab_files (id, registration_number, patient_id, urgent, status,"
                        + " created_at, updated_at, version) VALUES (?, ?, ?, false, 'OPEN',"
                        + " now(), now(), 0)",
                id,
                rn,
                patientId);
        return id;
    }

    private String seedAnalysisRequest(String labFileId, String analysisId, String analysisCode) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO analysis_requests (id, lab_file_id, analysis_id, analysis_code,"
                        + " requested_at, version) VALUES (?, ?, ?, ?, now(), 0)",
                id,
                labFileId,
                analysisId,
                analysisCode);
        return id;
    }

    private void seedResult(
            String labFileId, String arId, String analysisId, String analysisCode, String state) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO results (id, lab_file_id, analysis_request_id, analysis_id,"
                        + " analysis_code_snapshot, patient_category_snapshot,"
                        + " patient_sex_snapshot, state, created_at, updated_at, version)"
                        + " VALUES (?, ?, ?, ?, ?, 'FEMME', 'F', ?, now(), now(), 0)",
                id,
                labFileId,
                arId,
                analysisId,
                analysisCode,
                state);
    }
}
