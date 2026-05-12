package org.sparkcorporation.lims.result.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Full HTTP + Testcontainers acceptance IT for {@code GET /api/v1/results/anterior} per spec §IV
 * anterior-results lookup. Seeds a patient with 5 historical validated GLU results; asserts {@code
 * ?limit=3} returns the 3 most recent in DESC order; asserts component-code mismatch returns empty
 * list.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AnteriorResultIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;

    private RestClient http;
    private String patientId;
    private String analysisId;
    private String disciplineId;

    @BeforeEach
    void setUp() {
        http = restClientFor(port, "TECHNICIAN");
        patientId = seedPatient();
        disciplineId = seedDiscipline();
        analysisId = seedAnalysis("GLU", disciplineId);
    }

    @Test
    void limit_3_returns_three_most_recent_in_desc_order() {
        var labFileIds = new String[5];
        var now = Instant.now();
        for (int i = 0; i < 5; i++) {
            var labFileId = seedLabFile(patientId);
            labFileIds[i] = labFileId;
            var arId = seedAnalysisRequest(labFileId, analysisId, "GLU");
            var resultId = seedResult(labFileId, arId, analysisId, "GLU", "BIOLOGICAL_VALIDATED");
            seedResultLine(resultId, analysisId, "GLU");
            var bioValidatedAt = now.minus(5 - i, ChronoUnit.DAYS);
            jdbc.update(
                    "UPDATE results SET biological_validated_at = ? WHERE id = ?",
                    Timestamp.from(bioValidatedAt),
                    resultId);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows =
                http.get()
                        .uri(
                                "/api/v1/results/anterior?patientId={pid}&componentCode=GLU&limit=3",
                                patientId)
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});

        assertThat(rows).hasSize(3);
        var firstDate = (String) rows.get(0).get("biologicalValidatedAt");
        var secondDate = (String) rows.get(1).get("biologicalValidatedAt");
        var thirdDate = (String) rows.get(2).get("biologicalValidatedAt");
        assertThat(firstDate).isGreaterThan(secondDate);
        assertThat(secondDate).isGreaterThan(thirdDate);
    }

    @Test
    void mismatched_component_code_returns_empty_list() {
        var labFileId = seedLabFile(patientId);
        var arId = seedAnalysisRequest(labFileId, analysisId, "GLU");
        var resultId = seedResult(labFileId, arId, analysisId, "GLU", "BIOLOGICAL_VALIDATED");
        seedResultLine(resultId, analysisId, "GLU");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows =
                http.get()
                        .uri(
                                "/api/v1/results/anterior?patientId={pid}&componentCode=UNKNOWN",
                                patientId)
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});

        assertThat(rows).isEmpty();
    }

    @Test
    void missing_patient_id_returns_400() {
        var resp =
                http.get()
                        .uri("/api/v1/results/anterior?componentCode=GLU")
                        .retrieve()
                        .onStatus(status -> status.is4xxClientError(), (req, res) -> {})
                        .toEntity(String.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void server_side_limit_clamping_handles_within_range() {
        var labFileId = seedLabFile(patientId);
        var arId = seedAnalysisRequest(labFileId, analysisId, "GLU");
        var resultId = seedResult(labFileId, arId, analysisId, "GLU", "BIOLOGICAL_VALIDATED");
        seedResultLine(resultId, analysisId, "GLU");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows =
                http.get()
                        .uri(
                                "/api/v1/results/anterior?patientId={pid}&componentCode=GLU&limit=1",
                                patientId)
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});

        assertThat(rows).hasSize(1);
    }

    @Test
    void response_row_contains_expected_fields() {
        var labFileId = seedLabFile(patientId);
        var arId = seedAnalysisRequest(labFileId, analysisId, "GLU");
        var resultId = seedResult(labFileId, arId, analysisId, "GLU", "BIOLOGICAL_VALIDATED");
        seedResultLine(resultId, analysisId, "GLU");
        jdbc.update("UPDATE results SET biological_validated_at = now() WHERE id = ?", resultId);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows =
                http.get()
                        .uri(
                                "/api/v1/results/anterior?patientId={pid}&componentCode=GLU",
                                patientId)
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});

        assertThat(rows).isNotEmpty();
        var row = rows.get(0);
        assertThat(row)
                .containsKeys(
                        "resultId",
                        "lineId",
                        "labFileId",
                        "labFileRegistrationNumber",
                        "componentCode",
                        "resultType",
                        "isOutOfRange",
                        "biologicalValidatedAt");
        assertThat(row.get("componentCode")).isEqualTo("GLU");
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

    private String seedAnalysis(String code, String discId) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO analyses (id, code, name, kind, indiciary_value_b, label_count,"
                        + " discipline_id, result_type, version) VALUES (?, ?, ?, 'SIMPLE',"
                        + " 10.00, 1, ?, 'REAL', 0)",
                id,
                code + "-" + id.substring(10, 16),
                code,
                discId);
        return id;
    }

    private String seedLabFile(String pid) {
        var id = Ulid.generate();
        var seq = String.format("%04d", REG_SEQ.getAndIncrement());
        var rn = "LF-" + LocalDate.now().format(DATE_FMT) + "-" + seq;
        jdbc.update(
                "INSERT INTO lab_files (id, registration_number, patient_id, urgent, status,"
                        + " created_at, updated_at, version) VALUES (?, ?, ?, false, 'OPEN',"
                        + " now(), now(), 0)",
                id,
                rn,
                pid);
        return id;
    }

    private String seedAnalysisRequest(String labFileId, String aId, String analysisCode) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO analysis_requests (id, lab_file_id, analysis_id, analysis_code,"
                        + " requested_at, version) VALUES (?, ?, ?, ?, now(), 0)",
                id,
                labFileId,
                aId,
                analysisCode);
        return id;
    }

    private String seedResult(
            String labFileId, String arId, String aId, String analysisCode, String state) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO results (id, lab_file_id, analysis_request_id, analysis_id,"
                        + " analysis_code_snapshot, patient_category_snapshot,"
                        + " patient_sex_snapshot, state, created_at, updated_at, version)"
                        + " VALUES (?, ?, ?, ?, ?, 'FEMME', 'F', ?, now(), now(), 0)",
                id,
                labFileId,
                arId,
                aId,
                analysisCode,
                state);
        return id;
    }

    private void seedResultLine(String resultId, String componentAnalysisId, String componentCode) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO result_lines (id, result_id, component_analysis_id,"
                        + " component_code_snapshot, position, result_type, is_out_of_range,"
                        + " created_at, updated_at, version) VALUES (?, ?, ?, ?, 0, 'REAL',"
                        + " false, now(), now(), 0)",
                id,
                resultId,
                componentAnalysisId,
                componentCode);
    }
}
