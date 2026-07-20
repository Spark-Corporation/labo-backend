package org.sparkcorporation.lims.result.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.labsettings.api.LabSettingsApi;
import org.sparkcorporation.lims.labsettings.api.LabSettingsUpdateRequest;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * End-to-end acceptance IT for {@code GET /api/v1/paillasses/{disciplineId}/worksheet.pdf} (CDC
 * §VII per-paillasse worklist). Seeds two disciplines each with one analysis, lab files registered
 * today and yesterday with results + result lines, and asserts the PDFBox-extracted text reflects
 * the default-today window, discipline filtering, entered-value rendering, and access control.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaillasseWorksheetPdfAcceptanceIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    @LocalServerPort private int port;

    @Autowired private LabSettingsApi labSettingsApi;
    @Autowired private JdbcTemplate jdbc;

    private RestClient biologist;

    @BeforeEach
    void setUp() {
        biologist = restClientFor(port, "BIOLOGIST");
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
    void GET_returns200_pdf_withDisciplineRowsOnly_defaultToday() throws Exception {
        var fixture = seedFixture();

        var response =
                biologist
                        .get()
                        .uri("/api/v1/paillasses/{id}/worksheet.pdf", fixture.bioDisciplineId)
                        .retrieve()
                        .toEntity(byte[].class);

        var pdfBytes = response.getBody();
        assertThat(pdfBytes).isNotNull();
        assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");
        var disposition = response.getHeaders().getFirst("Content-Disposition");
        // The endpoint's default window is "today (UTC)" — assert in the same zone, or the
        // filename check breaks whenever the local calendar day differs from the UTC one.
        var today = LocalDate.now(ZoneOffset.UTC).format(ISO);
        assertThat(disposition)
                .contains(
                        "paillasse-%s-%s-%s.pdf"
                                .formatted(fixture.bioDisciplineCode, today, today));

        var text = extractText(pdfBytes);
        assertThat(text).contains("Biochimie");
        assertThat(text).contains(fixture.todayRn1);
        assertThat(text).contains(fixture.todayRn2);
        // entered value present (formatter emits a plain "1.45").
        assertThat(text).contains("1.45");
        // other-discipline analysis code excluded.
        assertThat(text).doesNotContain(fixture.hemCode);
        // yesterday's dossier excluded by the default-today window.
        assertThat(text).doesNotContain(fixture.yesterdayRn);
    }

    @Test
    void GET_withExplicitRange_includesYesterday() throws Exception {
        var fixture = seedFixture();
        var yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1).format(ISO);
        var today = LocalDate.now(ZoneOffset.UTC).format(ISO);

        var response =
                biologist
                        .get()
                        .uri(
                                "/api/v1/paillasses/{id}/worksheet.pdf?from={from}&to={to}",
                                fixture.bioDisciplineId,
                                yesterday,
                                today)
                        .retrieve()
                        .toEntity(byte[].class);

        var text = extractText(response.getBody());
        assertThat(text).contains(fixture.yesterdayRn);
    }

    @Test
    void GET_returns400_whenFromAfterTo() {
        var fixture = seedFixture();
        var yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1).format(ISO);
        var today = LocalDate.now(ZoneOffset.UTC).format(ISO);

        assertThatThrownBy(
                        () ->
                                biologist
                                        .get()
                                        .uri(
                                                "/api/v1/paillasses/{id}/worksheet.pdf?from={from}&to={to}",
                                                fixture.bioDisciplineId,
                                                today,
                                                yesterday)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(400)));
    }

    @Test
    void GET_returns404_unknownAndSoftDeletedDiscipline() {
        var unknown = "01HZX0000000000000NTFND0ZZ";
        assertThatThrownBy(
                        () ->
                                biologist
                                        .get()
                                        .uri("/api/v1/paillasses/{id}/worksheet.pdf", unknown)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(404)));

        var deletedDisciplineId = seedDeletedDiscipline();
        assertThatThrownBy(
                        () ->
                                biologist
                                        .get()
                                        .uri(
                                                "/api/v1/paillasses/{id}/worksheet.pdf",
                                                deletedDisciplineId)
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
        var fixture = seedFixture();

        assertThatThrownBy(
                        () ->
                                restClientAnonymous(port)
                                        .get()
                                        .uri(
                                                "/api/v1/paillasses/{id}/worksheet.pdf",
                                                fixture.bioDisciplineId)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(401)));
    }

    // ----- fixture --------------------------------------------------------------------------

    private record Fixture(
            String bioDisciplineId,
            String bioDisciplineCode,
            String todayRn1,
            String todayRn2,
            String yesterdayRn,
            String hemCode) {}

    private Fixture seedFixture() {
        var patientId = seedPatient();
        var bioDisciplineId = seedDiscipline("Biochimie", "BIOCHIMIE");
        var bioDisciplineCode = disciplineCodeOf(bioDisciplineId);
        var hemDisciplineId = seedDiscipline("Hématologie", "HEMATOLOGIE");
        var glyCode = uniqueCode("GLY");
        var creCode = uniqueCode("CRE");
        var hemCode = uniqueCode("NFS");
        var glyAnalysisId = seedAnalysis(glyCode, "Glucose", bioDisciplineId);
        var creAnalysisId = seedAnalysis(creCode, "Créatinine", bioDisciplineId);
        var hemAnalysisId = seedAnalysis(hemCode, "Numération", hemDisciplineId);

        // Today, biochimie, lab file 1 — one entered (out-of-range) line + one blank line.
        var labFile1 = seedLabFile(patientId, "now()");
        var ar1 = seedAnalysisRequest(labFile1, glyAnalysisId, glyCode);
        var result1 = seedResult(labFile1, ar1, glyAnalysisId, glyCode, "ENTERED");
        seedResultLine(result1, glyAnalysisId, glyCode, "1.45", true);
        var ar1b = seedAnalysisRequest(labFile1, creAnalysisId, creCode);
        var result1b = seedResult(labFile1, ar1b, creAnalysisId, creCode, "ENTERED");
        seedResultLine(result1b, creAnalysisId, creCode, null, false);
        seedSample(labFile1, glyAnalysisId, uniqueDailyId());
        var todayRn1 = registrationNumberOf(labFile1);

        // Today, biochimie, lab file 2.
        var labFile2 = seedLabFile(patientId, "now()");
        var ar2 = seedAnalysisRequest(labFile2, glyAnalysisId, glyCode);
        var result2 = seedResult(labFile2, ar2, glyAnalysisId, glyCode, "TECHNICAL_VALIDATED");
        seedResultLine(result2, glyAnalysisId, glyCode, "0.90", false);
        var todayRn2 = registrationNumberOf(labFile2);

        // Yesterday, biochimie — excluded from the default-today window.
        var labFile3 = seedLabFile(patientId, "now() - interval '1 day'");
        var ar3 = seedAnalysisRequest(labFile3, glyAnalysisId, glyCode);
        var result3 = seedResult(labFile3, ar3, glyAnalysisId, glyCode, "ENTERED");
        seedResultLine(result3, glyAnalysisId, glyCode, "0.85", false);
        var yesterdayRn = registrationNumberOf(labFile3);

        // Today, hématologie — proves discipline filtering excludes it from biochimie's sheet.
        var labFile4 = seedLabFile(patientId, "now()");
        var ar4 = seedAnalysisRequest(labFile4, hemAnalysisId, hemCode);
        var result4 = seedResult(labFile4, ar4, hemAnalysisId, hemCode, "ENTERED");
        seedResultLine(result4, hemAnalysisId, hemCode, "5.20", false);

        return new Fixture(
                bioDisciplineId, bioDisciplineCode, todayRn1, todayRn2, yesterdayRn, hemCode);
    }

    private String disciplineCodeOf(String disciplineId) {
        return jdbc.queryForObject(
                "SELECT code FROM disciplines WHERE id = ?", String.class, disciplineId);
    }

    private static String extractText(byte[] pdf) throws Exception {
        try (var doc = PDDocument.load(new ByteArrayInputStream(pdf))) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private String registrationNumberOf(String labFileId) {
        return jdbc.queryForObject(
                "SELECT registration_number FROM lab_files WHERE id = ?", String.class, labFileId);
    }

    private String uniqueDailyId() {
        // Format ^[12345]\d{6}-\d{3}$ — unique across tests via REG_SEQ to avoid the daily_id
        // unique constraint. Not asserted on in any test; only its presence drives the sample row.
        var seq = REG_SEQ.getAndIncrement();
        return "1" + String.format("%06d", seq % 1000000) + "-" + String.format("%03d", seq % 1000);
    }

    private String uniqueCode(String prefix) {
        // Short code so it fits the narrow worksheet "Code" column without PDFBox line-wrapping.
        return prefix + Ulid.generate().substring(23, 26);
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

    private String seedDeletedDiscipline() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO disciplines (id, code, name, discipline_prefix, deleted_at, version)"
                        + " VALUES (?, ?, 'Supprimée', 'BIOCHIMIE', now(), 0)",
                id,
                "DISC-" + id.substring(10, 20));
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

    private String seedLabFile(String patientId, String createdAtSql) {
        var id = Ulid.generate();
        var seq = String.format("%04d", REG_SEQ.getAndIncrement());
        var rn = "LF-" + LocalDate.now().format(DATE_FMT) + "-" + seq;
        jdbc.update(
                "INSERT INTO lab_files (id, registration_number, patient_id, urgent, status,"
                        + " created_at, updated_at, deleted_at, version) VALUES (?, ?, ?, false,"
                        + " 'OPEN', "
                        + createdAtSql
                        + ", now(), NULL, 0)",
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

    private void seedResultLine(
            String resultId,
            String componentAnalysisId,
            String componentCode,
            String valueReal,
            boolean outOfRange) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO result_lines (id, result_id, component_analysis_id,"
                    + " component_code_snapshot, position, result_type, value_real,"
                    + " is_out_of_range, created_at, updated_at, version) VALUES (?, ?, ?, ?, 1,"
                    + " 'REAL', ?, ?, now(), now(), 0)",
                id,
                resultId,
                componentAnalysisId,
                componentCode,
                valueReal == null ? null : new java.math.BigDecimal(valueReal),
                outOfRange);
    }

    private void seedSample(String labFileId, String analysisId, String dailyId) {
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
    }
}
