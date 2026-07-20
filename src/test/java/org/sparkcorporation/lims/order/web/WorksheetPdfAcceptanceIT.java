package org.sparkcorporation.lims.order.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
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
import org.sparkcorporation.lims.labsettings.api.LabSettingsApi;
import org.sparkcorporation.lims.labsettings.api.LabSettingsUpdateRequest;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * End-to-end acceptance IT for {@code GET /api/v1/lab-files/{id}/worksheet.pdf} (CDC §II « feuille
 * de paillasse »). Seeds a patient, two disciplines each with one analysis, a lab file, two
 * analysis requests, and one in-lab sample via raw JDBC; asserts the PDFBox-extracted text groups
 * analyses by paillasse and the access-control behaviour.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorksheetPdfAcceptanceIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    @LocalServerPort private int port;

    @Autowired private LabSettingsApi labSettingsApi;
    @Autowired private JdbcTemplate jdbc;

    private RestClient admin;

    @BeforeEach
    void setUp() {
        admin = restClientFor(port, "ADMIN");
        labSettingsApi.update(
                new LabSettingsUpdateRequest(
                        "Lab Sparkcorp", "12 Avenue du Test, Lomé", "ACC-2026-001", "Dr. Manager"));
    }

    @AfterEach
    void restoreLabSettingsSeed() {
        labSettingsApi.update(
                new LabSettingsUpdateRequest(
                        "Laboratoire",
                        "Adresse à configurer",
                        "Numéro d'accréditation à configurer",
                        "Directeur à configurer"));
    }

    @Test
    void GET_returns200_pdfGroupedByPaillasse() throws Exception {
        var patientId = seedPatient();
        var bioDisciplineId = seedDiscipline("Biochimie", "BIOCHIMIE");
        var hemDisciplineId = seedDiscipline("Hématologie", "HEMATOLOGIE");
        var glyCode = uniqueAnalysisCode("GLY");
        var nfsCode = uniqueAnalysisCode("NFS");
        var glyAnalysisId = seedAnalysis(glyCode, "Glucose", bioDisciplineId);
        var nfsAnalysisId = seedAnalysis(nfsCode, "Numération", hemDisciplineId);
        var labFileId = seedLabFile(patientId, false);
        seedAnalysisRequest(labFileId, glyAnalysisId, glyCode);
        seedAnalysisRequest(labFileId, nfsAnalysisId, nfsCode);
        var dailyId = uniqueDailyId();
        seedSample(labFileId, glyAnalysisId, dailyId);

        var response =
                admin.get()
                        .uri("/api/v1/lab-files/{id}/worksheet.pdf", labFileId)
                        .retrieve()
                        .toEntity(byte[].class);

        var pdfBytes = response.getBody();
        assertThat(pdfBytes).isNotNull();
        assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");
        var disposition = response.getHeaders().getFirst("Content-Disposition");
        assertThat(disposition).contains("feuille-paillasse-");

        var text = extractText(pdfBytes);
        assertThat(text).contains("Feuille de paillasse");
        assertThat(text).contains("Biochimie");
        assertThat(text).contains("Hématologie");
        assertThat(text).contains(glyCode);
        assertThat(text).contains(dailyId);
        assertThat(text).contains("DOE");
    }

    @Test
    void GET_returns404_unknownAndDeletedLabFile() {
        var unknown = "01HZX0000000000000NTFND0ZZ";
        assertThatThrownBy(
                        () ->
                                admin.get()
                                        .uri("/api/v1/lab-files/{id}/worksheet.pdf", unknown)
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

        var patientId = seedPatient();
        var deletedLabFileId = seedDeletedLabFile(patientId);
        assertThatThrownBy(
                        () ->
                                admin.get()
                                        .uri(
                                                "/api/v1/lab-files/{id}/worksheet.pdf",
                                                deletedLabFileId)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(404)));
    }

    @Test
    void GET_returns401_anonymous() {
        var patientId = seedPatient();
        var labFileId = seedLabFile(patientId, false);

        assertThatThrownBy(
                        () ->
                                restClientAnonymous(port)
                                        .get()
                                        .uri("/api/v1/lab-files/{id}/worksheet.pdf", labFileId)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(401)));
    }

    // ----- helpers --------------------------------------------------------------------------

    private static String extractText(byte[] pdf) throws Exception {
        try (var doc = PDDocument.load(new ByteArrayInputStream(pdf))) {
            return new PDFTextStripper().getText(doc);
        }
    }

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

    private String uniqueAnalysisCode(String prefix) {
        // Short code so it fits the narrow worksheet "Code" column without PDFBox line-wrapping
        // (a wrapped code would split across lines and break the contains-assertion).
        return prefix + Ulid.generate().substring(23, 26);
    }

    /** Unique daily ID matching {@code ^[12345]\d{6}-\d{3}$}; shared-container-safe via REG_SEQ. */
    private String uniqueDailyId() {
        return "1" + String.format("%06d", REG_SEQ.getAndIncrement()) + "-001";
    }

    private String seedDiscipline(String name, String prefix) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO disciplines (id, code, name, discipline_prefix, version) VALUES (?,"
                        + " ?, ?, ?, 0)",
                id,
                "DISC-" + id.substring(10, 20),
                name,
                prefix);
        return id;
    }

    private String seedAnalysis(String code, String name, String disciplineId) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO analyses (id, code, name, kind, indiciary_value_b, label_count,"
                        + " discipline_id, result_type, version) VALUES (?, ?, ?, 'SIMPLE', 10.00,"
                        + " 1, ?, 'REAL', 0)",
                id,
                code,
                name,
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
