package org.sparkcorporation.lims.order.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * End-to-end acceptance IT for {@code GET /api/v1/lab-files/{id}/labels} (CDC §IV). Seeds a
 * patient, a discipline, an analysis with {@code label_count = 2}, a lab file, an analysis request,
 * and an in-lab sample via raw JDBC; asserts the JSON label-record shape and the access-control
 * behaviour.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LabelControllerIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;

    private RestClient receptionist;

    @BeforeEach
    void setUp() {
        receptionist = restClientFor(port, "RECEPTIONIST");
    }

    @Test
    void GET_returns200_withLabelRecordShape() {
        var patientId = seedPatient();
        var disciplineCode = uniqueDisciplineCode();
        var disciplineId = seedDiscipline(disciplineCode, "BIOCHIMIE");
        var analysisCode = uniqueAnalysisCode();
        var analysisId = seedAnalysis(analysisCode, disciplineId);
        var labFileId = seedLabFile(patientId, false);
        seedAnalysisRequest(labFileId, analysisId, analysisCode);
        var dailyId = uniqueDailyId();
        seedSample(labFileId, analysisId, dailyId);

        var response =
                receptionist
                        .get()
                        .uri("/api/v1/lab-files/{id}/labels", labFileId)
                        .retrieve()
                        .body(LabelResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.labFileId()).isEqualTo(labFileId);
        assertThat(response.labels()).hasSize(1);
        var label = response.labels().get(0);
        assertThat(label.dailyId()).isEqualTo(dailyId);
        assertThat(label.copies()).isEqualTo(2);
        assertThat(label.discipline()).isEqualTo(disciplineCode);
        assertThat(label.patient().nom()).isEqualTo("DOE");
        assertThat(label.patient().dateNaissance()).isEqualTo("1985-07-12");
        assertThat(label.analyses()).contains(analysisCode);
    }

    @Test
    void GET_returns200_emptyLabels_whenNoSamples() {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline(uniqueDisciplineCode(), "BIOCHIMIE");
        var analysisCode = uniqueAnalysisCode();
        var analysisId = seedAnalysis(analysisCode, disciplineId);
        var labFileId = seedLabFile(patientId, false);
        seedAnalysisRequest(labFileId, analysisId, analysisCode);

        var response =
                receptionist
                        .get()
                        .uri("/api/v1/lab-files/{id}/labels", labFileId)
                        .retrieve()
                        .body(LabelResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.labels()).isEmpty();
    }

    @Test
    void GET_returns404_forUnknownLabFile() {
        var unknown = "01HZX0000000000000NTFND0ZZ";

        assertThatThrownBy(
                        () ->
                                receptionist
                                        .get()
                                        .uri("/api/v1/lab-files/{id}/labels", unknown)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex -> {
                            assertThat(ex.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(404));
                            assertThat(ex.getResponseHeaders().getContentType())
                                    .isNotNull()
                                    .satisfies(
                                            ct ->
                                                    assertThat(ct.toString())
                                                            .startsWith(
                                                                    MediaType
                                                                            .APPLICATION_PROBLEM_JSON_VALUE));
                        });
    }

    @Test
    void GET_returns404_forSoftDeletedLabFile() {
        var patientId = seedPatient();
        var labFileId = seedDeletedLabFile(patientId);

        assertThatThrownBy(
                        () ->
                                receptionist
                                        .get()
                                        .uri("/api/v1/lab-files/{id}/labels", labFileId)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(404)));
    }

    @Test
    void GET_returns403_withoutPermission() {
        var patientId = seedPatient();
        var labFileId = seedLabFile(patientId, false);

        // CASHIER does not hold lab-file.read (see 202605061001-seed-iam-catalog.yaml).
        var cashier = restClientFor(port, "CASHIER");
        assertThatThrownBy(
                        () ->
                                cashier.get()
                                        .uri("/api/v1/lab-files/{id}/labels", labFileId)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(403)));

        // Anonymous callers are rejected outright.
        assertThatThrownBy(
                        () ->
                                restClientAnonymous(port)
                                        .get()
                                        .uri("/api/v1/lab-files/{id}/labels", labFileId)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(401)));
    }

    // ----- seed helpers ---------------------------------------------------------------------

    private String seedPatient() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO patients (id, lab_code, last_name, first_names, civility, sex,"
                    + " birth_date, insured, version) VALUES (?, ?, 'DOE', 'Jane', 'MADAME', 'F',"
                    + " DATE '1985-07-12', false, 0)",
                id,
                "LAB-" + id.substring(0, 10));
        return id;
    }

    private String uniqueDisciplineCode() {
        return "DISC-" + Ulid.generate().substring(10, 20);
    }

    private String uniqueAnalysisCode() {
        return "GLY-" + Ulid.generate().substring(10, 18);
    }

    /** Unique daily ID matching {@code ^[12345]\d{6}-\d{3}$}; shared-container-safe via REG_SEQ. */
    private String uniqueDailyId() {
        return "1" + String.format("%06d", REG_SEQ.getAndIncrement()) + "-001";
    }

    private String seedDiscipline(String code, String prefix) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO disciplines (id, code, name, discipline_prefix, version) VALUES (?,"
                        + " ?, 'Biochimie', ?, 0)",
                id,
                code,
                prefix);
        return id;
    }

    private String seedAnalysis(String code, String disciplineId) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO analyses (id, code, name, kind, indiciary_value_b, label_count,"
                        + " discipline_id, result_type, version) VALUES (?, ?, 'Glucose', 'SIMPLE',"
                        + " 10.00, 2, ?, 'REAL', 0)",
                id,
                code,
                disciplineId);
        return id;
    }

    private String seedRecipient() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO recipients (id, code, name, version) VALUES (?, ?, 'Tube EDTA', 0)",
                id,
                "TUBE-" + id.substring(10, 16));
        return id;
    }

    private String seedLabFile(String patientId, boolean urgent) {
        var id = Ulid.generate();
        var seq = String.format("%04d", REG_SEQ.getAndIncrement());
        var rn = "LF-" + LocalDate.now().format(DATE_FMT) + "-" + seq;
        jdbc.update(
                "INSERT INTO lab_files (id, registration_number, patient_id, urgent, status,"
                        + " created_at, updated_at, deleted_at, version) VALUES (?, ?, ?, ?,"
                        + " 'OPEN', now(), now(), NULL, 0)",
                id,
                rn,
                patientId,
                urgent);
        return id;
    }

    private String seedDeletedLabFile(String patientId) {
        var id = Ulid.generate();
        var seq = String.format("%04d", REG_SEQ.getAndIncrement());
        var rn = "LF-" + LocalDate.now().format(DATE_FMT) + "-" + seq;
        jdbc.update(
                "INSERT INTO lab_files (id, registration_number, patient_id, urgent, status,"
                        + " created_at, updated_at, deleted_at, version) VALUES (?, ?, ?, false,"
                        + " 'OPEN', now(), now(), now(), 0)",
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

    private String seedSample(String labFileId, String analysisId, String dailyId) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO samples (id, sample_type, daily_id, lab_file_id, analysis_id,"
                        + " recipient_id, collected_at, version) VALUES (?, 'IN_LAB', ?, ?, ?, ?,"
                        + " now(), 0)",
                id,
                dailyId,
                labFileId,
                analysisId,
                seedRecipient());
        return id;
    }
}
