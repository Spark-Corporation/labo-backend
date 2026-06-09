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
 * Full HTTP + Testcontainers acceptance IT for {@code GET /api/v1/dossiers} per spec §IX
 * state-filter dossier list. Seeds mixed-state dossiers including soft-deleted; asserts each filter
 * value returns expected subset; explicitly tests {@code state=DELETED} returns ONLY soft-deleted.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DossierListIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;

    private RestClient http;
    private String validatedLabFileId;
    private String notValidatedLabFileId;
    private String inProgressLabFileId;
    private String deletedLabFileId;

    @BeforeEach
    void setUp() {
        http = restClientFor(port, "BIOLOGIST");
        seedFixture();
    }

    @Test
    void no_filters_returns_active_dossiers_only() {
        @SuppressWarnings("rawtypes")
        var resp = http.get().uri("/api/v1/dossiers?size=200").retrieve().toEntity(Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        var content = (List<Map<?, ?>>) resp.getBody().get("content");
        var ids = content.stream().map(r -> (String) r.get("labFileId")).toList();
        assertThat(ids).contains(validatedLabFileId, notValidatedLabFileId, inProgressLabFileId);
        assertThat(ids).doesNotContain(deletedLabFileId);
    }

    @Test
    void state_validated_returns_only_fully_bio_validated_dossiers() {
        @SuppressWarnings("rawtypes")
        var resp =
                http.get()
                        .uri("/api/v1/dossiers?state=VALIDATED&size=200")
                        .retrieve()
                        .toEntity(Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        var content = (List<Map<?, ?>>) resp.getBody().get("content");
        var ids = content.stream().map(r -> (String) r.get("labFileId")).toList();
        assertThat(ids).contains(validatedLabFileId);
        assertThat(ids)
                .doesNotContain(notValidatedLabFileId, inProgressLabFileId, deletedLabFileId);
    }

    @Test
    void state_deleted_returns_only_soft_deleted_dossiers_not_a_union() {
        @SuppressWarnings("rawtypes")
        var resp =
                http.get()
                        .uri("/api/v1/dossiers?state=DELETED&size=200")
                        .retrieve()
                        .toEntity(Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        var content = (List<Map<?, ?>>) resp.getBody().get("content");
        var ids = content.stream().map(r -> (String) r.get("labFileId")).toList();
        assertThat(ids).contains(deletedLabFileId);
        assertThat(ids)
                .doesNotContain(validatedLabFileId, notValidatedLabFileId, inProgressLabFileId);
    }

    @Test
    void state_in_progress_returns_dossiers_with_entered_or_tech_validated_results() {
        @SuppressWarnings("rawtypes")
        var resp =
                http.get()
                        .uri("/api/v1/dossiers?state=IN_PROGRESS&size=200")
                        .retrieve()
                        .toEntity(Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        var content = (List<Map<?, ?>>) resp.getBody().get("content");
        var ids = content.stream().map(r -> (String) r.get("labFileId")).toList();
        assertThat(ids).contains(inProgressLabFileId);
        assertThat(ids).doesNotContain(validatedLabFileId, deletedLabFileId);
    }

    @Test
    void bad_state_enum_returns_400() throws Exception {
        var resp =
                http.get()
                        .uri("/api/v1/dossiers?state=UNKNOWN_STATE")
                        .retrieve()
                        .onStatus(status -> status.is4xxClientError(), (req, res) -> {})
                        .toEntity(String.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void printed_true_returnsOnlyPrintedDossiers() {
        var patientId = seedPatient();
        var printed = seedLabFile(patientId, false, false, "OPEN", true, false);
        var notPrinted = seedLabFile(patientId, false, false, "OPEN", false, false);

        var ids = listIds("/api/v1/dossiers?printed=true&size=200");

        assertThat(ids).contains(printed).doesNotContain(notPrinted);
    }

    @Test
    void printed_false_excludesPrinted() {
        var patientId = seedPatient();
        var printed = seedLabFile(patientId, false, false, "OPEN", true, false);
        var notPrinted = seedLabFile(patientId, false, false, "OPEN", false, false);

        var ids = listIds("/api/v1/dossiers?printed=false&size=200");

        assertThat(ids).contains(notPrinted).doesNotContain(printed);
    }

    @Test
    void emailed_true_returnsOnlyEmailedDossiers() {
        var patientId = seedPatient();
        var emailed = seedLabFile(patientId, false, false, "OPEN", false, true);
        var notEmailed = seedLabFile(patientId, false, false, "OPEN", false, false);

        var ids = listIds("/api/v1/dossiers?emailed=true&size=200");

        assertThat(ids).contains(emailed).doesNotContain(notEmailed);
    }

    @Test
    void signed_true_returnsSignedAndPublished() {
        var patientId = seedPatient();
        var signed = seedLabFile(patientId, false, false, "SIGNED", false, false);
        var published = seedLabFile(patientId, false, false, "PUBLISHED", false, false);
        var open = seedLabFile(patientId, false, false, "OPEN", false, false);

        var ids = listIds("/api/v1/dossiers?signed=true&size=200");

        assertThat(ids).contains(signed, published).doesNotContain(open);
    }

    @Test
    void signed_false_returnsTheOpenOne() {
        var patientId = seedPatient();
        var signed = seedLabFile(patientId, false, false, "SIGNED", false, false);
        var open = seedLabFile(patientId, false, false, "OPEN", false, false);

        var ids = listIds("/api/v1/dossiers?signed=false&size=200");

        assertThat(ids).contains(open).doesNotContain(signed);
    }

    @Test
    void filtersCombine_printedAndUrgent() {
        var patientId = seedPatient();
        var printedUrgent = seedLabFile(patientId, true, false, "OPEN", true, false);
        var printedNotUrgent = seedLabFile(patientId, false, false, "OPEN", true, false);
        var urgentNotPrinted = seedLabFile(patientId, true, false, "OPEN", false, false);

        var ids = listIds("/api/v1/dossiers?printed=true&urgent=true&size=200");

        assertThat(ids).contains(printedUrgent).doesNotContain(printedNotUrgent, urgentNotPrinted);
    }

    @Test
    void rows_carryPrintedAtAndEmailedAt() {
        var patientId = seedPatient();
        var stamped = seedLabFile(patientId, false, false, "OPEN", true, true);
        var virgin = seedLabFile(patientId, false, false, "OPEN", false, false);

        @SuppressWarnings("rawtypes")
        var resp = http.get().uri("/api/v1/dossiers?size=200").retrieve().toEntity(Map.class);
        @SuppressWarnings("unchecked")
        var content = (List<Map<?, ?>>) resp.getBody().get("content");

        var stampedRow =
                content.stream()
                        .filter(r -> stamped.equals(r.get("labFileId")))
                        .findFirst()
                        .orElseThrow();
        var virginRow =
                content.stream()
                        .filter(r -> virgin.equals(r.get("labFileId")))
                        .findFirst()
                        .orElseThrow();
        assertThat(stampedRow.get("printedAt")).isNotNull();
        assertThat(stampedRow.get("emailedAt")).isNotNull();
        assertThat(virginRow.get("printedAt")).isNull();
        assertThat(virginRow.get("emailedAt")).isNull();
    }

    private List<String> listIds(String uri) {
        @SuppressWarnings("rawtypes")
        var resp = http.get().uri(uri).retrieve().toEntity(Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        var content = (List<Map<?, ?>>) resp.getBody().get("content");
        return content.stream().map(r -> (String) r.get("labFileId")).toList();
    }

    private void seedFixture() {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        var analysisId = seedAnalysis("GLU", disciplineId);

        validatedLabFileId = seedLabFile(patientId, false, false);
        var arValidated = seedAnalysisRequest(validatedLabFileId, analysisId, "GLU");
        seedResult(validatedLabFileId, arValidated, analysisId, "GLU", "BIOLOGICAL_VALIDATED");

        notValidatedLabFileId = seedLabFile(patientId, false, false);
        var arNotValidated = seedAnalysisRequest(notValidatedLabFileId, analysisId, "GLU");
        seedResult(notValidatedLabFileId, arNotValidated, analysisId, "GLU", "DRAFT");

        inProgressLabFileId = seedLabFile(patientId, false, false);
        var arInProgress = seedAnalysisRequest(inProgressLabFileId, analysisId, "GLU");
        seedResult(inProgressLabFileId, arInProgress, analysisId, "GLU", "TECHNICAL_VALIDATED");

        deletedLabFileId = seedLabFile(patientId, false, true);
        var arDeleted = seedAnalysisRequest(deletedLabFileId, analysisId, "GLU");
        seedResult(deletedLabFileId, arDeleted, analysisId, "GLU", "DRAFT");
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

    private String seedLabFile(String patientId, boolean urgent, boolean deleted) {
        return seedLabFile(patientId, urgent, deleted, "OPEN", false, false);
    }

    private String seedLabFile(
            String patientId,
            boolean urgent,
            boolean deleted,
            String status,
            boolean printed,
            boolean emailed) {
        var id = Ulid.generate();
        var seq = String.format("%04d", REG_SEQ.getAndIncrement());
        var rn = "LF-" + LocalDate.now().format(DATE_FMT) + "-" + seq;
        var deletedAt = deleted ? "now()" : "NULL";
        var printedAt = printed ? "now()" : "NULL";
        var emailedAt = emailed ? "now()" : "NULL";
        jdbc.update(
                "INSERT INTO lab_files (id, registration_number, patient_id, urgent, status,"
                        + " created_at, updated_at, deleted_at, printed_at, emailed_at, version)"
                        + " VALUES (?, ?, ?, ?, ?, now(), now(), "
                        + deletedAt
                        + ", "
                        + printedAt
                        + ", "
                        + emailedAt
                        + ", 0)",
                id,
                rn,
                patientId,
                urgent,
                status);
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
        return id;
    }
}
