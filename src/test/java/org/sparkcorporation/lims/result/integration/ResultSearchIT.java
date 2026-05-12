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
 * Full HTTP + Testcontainers acceptance IT for {@code GET /api/v1/results/search} per spec §V.
 * Mirrors {@link ResultEntryAndValidationIT} for seed pattern and auth setup.
 *
 * <p>Seeds 2 patients (1 insured / 1 not), 2 lab-files, 2 results (GLU bio-validated, CBC draft),
 * each with 1 result line. Exercises no-filter, analysisCode, patientType, resultState filters, and
 * the inverted-bounds 400.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResultSearchIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;

    private RestClient http;

    @BeforeEach
    void setUp() {
        http = restClientFor(port, "TECHNICIAN");
        seedTwoPatientsAndResults();
    }

    @Test
    void search_with_no_filters_returns_line_level_rows() {
        @SuppressWarnings("rawtypes")
        var resp = http.get().uri("/api/v1/results/search").retrieve().toEntity(Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(((Number) body.get("totalElements")).intValue()).isGreaterThan(0);
        @SuppressWarnings("unchecked")
        var content = (List<Map<?, ?>>) body.get("content");
        assertThat(content).isNotEmpty();
        var row = content.get(0);
        assertThat(row.containsKey("analysisCode")).isTrue();
        assertThat(row.containsKey("componentCode")).isTrue();
        assertThat(row.containsKey("labFileRegistrationNumber")).isTrue();
    }

    @Test
    void search_filters_by_analysis_code() {
        @SuppressWarnings("rawtypes")
        var resp =
                http.get()
                        .uri("/api/v1/results/search?analysisCode=GLU")
                        .retrieve()
                        .toEntity(Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = resp.getBody();
        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        var content = (List<Map<?, ?>>) body.get("content");
        assertThat(content).isNotEmpty();
        content.forEach(row -> assertThat(row.get("analysisCode")).isEqualTo("GLU"));
    }

    @Test
    void search_filters_by_insured_patient_type() {
        @SuppressWarnings("rawtypes")
        var resp =
                http.get()
                        .uri("/api/v1/results/search?patientType=INSURED")
                        .retrieve()
                        .toEntity(Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = resp.getBody();
        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        var content = (List<Map<?, ?>>) body.get("content");
        assertThat(content).isNotEmpty();
        content.forEach(row -> assertThat(row.get("patientInsured")).isEqualTo(true));
    }

    @Test
    void search_filters_by_validated_result_state() {
        @SuppressWarnings("rawtypes")
        var resp =
                http.get()
                        .uri("/api/v1/results/search?resultState=VALIDATED")
                        .retrieve()
                        .toEntity(Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = resp.getBody();
        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        var content = (List<Map<?, ?>>) body.get("content");
        assertThat(content).isNotEmpty();
        content.forEach(
                row -> assertThat(row.get("resultState")).isEqualTo("BIOLOGICAL_VALIDATED"));
    }

    @Test
    void inverted_date_bounds_returns_400() {
        var resp =
                http.get()
                        .uri("/api/v1/results/search?dateFrom=2026-05-10&dateTo=2026-05-01")
                        .retrieve()
                        .onStatus(s -> s.is4xxClientError(), (request, response) -> {})
                        .toEntity(String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unauthenticated_request_returns_401() {
        var anonymousClient = RestClient.builder().baseUrl("http://localhost:" + port).build();
        var resp =
                anonymousClient
                        .get()
                        .uri("/api/v1/results/search")
                        .retrieve()
                        .onStatus(s -> s.is4xxClientError(), (request, response) -> {})
                        .toEntity(String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void wrong_permission_returns_403() {
        var receptionist = restClientFor(port, "RECEPTIONIST");
        var resp =
                receptionist
                        .get()
                        .uri("/api/v1/results/search")
                        .retrieve()
                        .onStatus(s -> s.is4xxClientError(), (request, response) -> {})
                        .toEntity(String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private void seedTwoPatientsAndResults() {
        var insuredPatientId = seedPatient(true);
        var notInsuredPatientId = seedPatient(false);
        var disciplineId = seedDiscipline();
        var analysisGluId = seedAnalysis("GLU", disciplineId);
        var analysisCbcId = seedAnalysis("CBC", disciplineId);
        var labFile1 = seedLabFile(insuredPatientId);
        var labFile2 = seedLabFile(notInsuredPatientId);
        var arGlu = seedAnalysisRequest(labFile1, analysisGluId, "GLU");
        var arCbc = seedAnalysisRequest(labFile2, analysisCbcId, "CBC");
        var resultGlu = seedResult(labFile1, arGlu, analysisGluId, "GLU", "BIOLOGICAL_VALIDATED");
        var resultCbc = seedResult(labFile2, arCbc, analysisCbcId, "CBC", "DRAFT");
        seedResultLine(resultGlu, analysisGluId, "GLU");
        seedResultLine(resultCbc, analysisCbcId, "CBC");
    }

    private String seedPatient(boolean insured) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO patients (id, lab_code, last_name, civility, sex, insured, version)"
                        + " VALUES (?, ?, 'DOE', 'MADAME', 'F', ?, 0)",
                id,
                "LAB-" + id.substring(0, 26),
                insured);
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

    private String seedResult(
            String labFileId, String arId, String analysisId, String analysisCode, String state) {
        var id = Ulid.generate();
        var biovalAt = "BIOLOGICAL_VALIDATED".equals(state) ? "now()" : "NULL";
        jdbc.update(
                "INSERT INTO results (id, lab_file_id, analysis_request_id, analysis_id,"
                        + " analysis_code_snapshot, patient_category_snapshot,"
                        + " patient_sex_snapshot, state, biological_validated_at,"
                        + " created_at, updated_at, version)"
                        + " VALUES (?, ?, ?, ?, ?, 'FEMME', 'F', ?, "
                        + biovalAt
                        + ", now(), now(), 0)",
                id,
                labFileId,
                arId,
                analysisId,
                analysisCode,
                state);
        return id;
    }

    private void seedResultLine(String resultId, String componentAnalysisId, String componentCode) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO result_lines (id, result_id, component_analysis_id,"
                        + " component_code_snapshot, position, result_type, is_out_of_range,"
                        + " created_at, updated_at, version) VALUES (?, ?, ?, ?, 1, 'REAL', false,"
                        + " now(), now(), 0)",
                id,
                resultId,
                componentAnalysisId,
                componentCode);
    }
}
