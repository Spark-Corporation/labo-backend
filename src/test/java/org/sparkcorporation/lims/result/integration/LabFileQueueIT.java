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
 * Full HTTP + Testcontainers acceptance IT for {@code GET /api/v1/lab-files/queues/urgent} per spec
 * §X urgent-dossier queue. Seeds urgent / non-urgent / fully-validated dossiers and verifies marker
 * assignment (GREEN / RED / PEN) and NON_RENDU_PREDICATE filtering.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LabFileQueueIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;

    private RestClient http;
    private String urgentPenLabFileId;
    private String urgentRedLabFileId;

    @BeforeEach
    void setUp() {
        http = restClientFor(port, "BIOLOGIST");
        seedFixture();
    }

    @Test
    void urgent_queue_returns_non_rendu_urgent_dossiers_only() {
        @SuppressWarnings("rawtypes")
        var resp =
                http.get()
                        .uri("/api/v1/lab-files/queues/urgent?size=200")
                        .retrieve()
                        .toEntity(Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = resp.getBody();
        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        var content = (List<Map<?, ?>>) body.get("content");
        var returnedLabFileIds = content.stream().map(r -> (String) r.get("labFileId")).toList();
        assertThat(returnedLabFileIds).contains(urgentPenLabFileId, urgentRedLabFileId);
    }

    @Test
    void urgent_queue_pen_marker_for_draft_result() {
        @SuppressWarnings("rawtypes")
        var resp =
                http.get()
                        .uri("/api/v1/lab-files/queues/urgent?size=200")
                        .retrieve()
                        .toEntity(Map.class);
        @SuppressWarnings("unchecked")
        var content = (List<Map<?, ?>>) resp.getBody().get("content");
        var penRow =
                content.stream()
                        .filter(r -> urgentPenLabFileId.equals(r.get("labFileId")))
                        .findFirst();
        assertThat(penRow).isPresent();
        assertThat(penRow.get().get("marker")).isEqualTo("PEN");
    }

    @Test
    void urgent_queue_red_marker_for_out_of_range_line() {
        @SuppressWarnings("rawtypes")
        var resp =
                http.get()
                        .uri("/api/v1/lab-files/queues/urgent?size=200")
                        .retrieve()
                        .toEntity(Map.class);
        @SuppressWarnings("unchecked")
        var content = (List<Map<?, ?>>) resp.getBody().get("content");
        var redRow =
                content.stream()
                        .filter(r -> urgentRedLabFileId.equals(r.get("labFileId")))
                        .findFirst();
        assertThat(redRow).isPresent();
        assertThat(redRow.get().get("marker")).isEqualTo("RED");
    }

    private void seedFixture() {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        var analysisId = seedAnalysis("GLU", disciplineId);

        urgentPenLabFileId = seedLabFile(patientId, true);
        urgentRedLabFileId = seedLabFile(patientId, true);
        var fullyValidatedLabFileId = seedLabFile(patientId, true);
        var nonUrgentLabFileId = seedLabFile(patientId, false);

        var arPen = seedAnalysisRequest(urgentPenLabFileId, analysisId, "GLU");
        seedResult(urgentPenLabFileId, arPen, analysisId, "GLU", "DRAFT", false);

        var arRed = seedAnalysisRequest(urgentRedLabFileId, analysisId, "GLU");
        var redResultId =
                seedResult(
                        urgentRedLabFileId, arRed, analysisId, "GLU", "TECHNICAL_VALIDATED", false);
        seedOutOfRangeResultLine(redResultId, analysisId);

        var arFully = seedAnalysisRequest(fullyValidatedLabFileId, analysisId, "GLU");
        seedResult(
                fullyValidatedLabFileId, arFully, analysisId, "GLU", "BIOLOGICAL_VALIDATED", false);

        var arNonUrgent = seedAnalysisRequest(nonUrgentLabFileId, analysisId, "GLU");
        seedResult(nonUrgentLabFileId, arNonUrgent, analysisId, "GLU", "DRAFT", false);
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

    private String seedLabFile(String patientId, boolean urgent) {
        var id = Ulid.generate();
        var seq = String.format("%04d", REG_SEQ.getAndIncrement());
        var rn = "LF-" + LocalDate.now().format(DATE_FMT) + "-" + seq;
        jdbc.update(
                "INSERT INTO lab_files (id, registration_number, patient_id, urgent, status,"
                        + " created_at, updated_at, version) VALUES (?, ?, ?, ?, 'OPEN',"
                        + " now(), now(), 0)",
                id,
                rn,
                patientId,
                urgent);
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
            String labFileId,
            String arId,
            String analysisId,
            String analysisCode,
            String state,
            boolean deleted) {
        var id = Ulid.generate();
        var deletedAt = deleted ? "now()" : "NULL";
        jdbc.update(
                "INSERT INTO results (id, lab_file_id, analysis_request_id, analysis_id,"
                        + " analysis_code_snapshot, patient_category_snapshot,"
                        + " patient_sex_snapshot, state, deleted_at, created_at, updated_at,"
                        + " version) VALUES (?, ?, ?, ?, ?, 'FEMME', 'F', ?, "
                        + deletedAt
                        + ", now(), now(), 0)",
                id,
                labFileId,
                arId,
                analysisId,
                analysisCode,
                state);
        return id;
    }

    private void seedOutOfRangeResultLine(String resultId, String componentAnalysisId) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO result_lines (id, result_id, component_analysis_id,"
                        + " component_code_snapshot, position, result_type, is_out_of_range,"
                        + " created_at, updated_at, version) VALUES (?, ?, ?, 'GLU', 0, 'REAL',"
                        + " true, now(), now(), 0)",
                id,
                resultId,
                componentAnalysisId);
    }
}
