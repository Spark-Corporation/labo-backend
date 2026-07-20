package org.sparkcorporation.lims.result.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.labsettings.api.LabSettingsApi;
import org.sparkcorporation.lims.labsettings.api.LabSettingsUpdateRequest;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.api.ResultApi;
import org.sparkcorporation.lims.result.api.ResultState;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * End-to-end acceptance IT for the CR PDF endpoint (chunk 6 spec FFT #4). Seeds a patient + a
 * discipline + N analyses via raw SQL, opens a lab file via the order API, waits for the
 * AFTER_COMMIT result eager-creation, enters values, validates technically then biologically,
 * finally fetches the rendered PDF and asserts its bytes + extracted text.
 *
 * <p>Mirrors the seeding pattern of {@code ResultEntryAndValidationIT}; the only difference is the
 * concluding {@code GET /api/v1/results/{labFileId}/pdf} call.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResultCrPdfAcceptanceIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ResultApi resultApi;
    @Autowired private LabSettingsApi labSettingsApi;

    private RestClient admin;
    private RestClient technician;
    private RestClient biologist;

    @BeforeEach
    void setUp() {
        admin = restClientFor(port, "ADMIN");
        technician = restClientFor(port, "TECHNICIAN");
        biologist = restClientFor(port, "BIOLOGIST");
    }

    // Restore the Liquibase seed values so subsequent IT classes inherit a known state
    // (mirrors LabSettingsControllerIT chunk-3 pattern).
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
    void endToEndRenderForValidatedDossier() throws Exception {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline("Biochimie", "BIOCHIMIE");
        var analysisCode = "GLU_" + Ulid.generate().substring(10, 16);
        seedAnalysis(analysisCode, "Glucose", disciplineId);

        var labFileId = openLabFile(patientId, List.of(analysisCode));

        // Wait for AFTER_COMMIT result eager-creation, then drive the result through the state
        // machine.
        var resultIds = waitForResultsCreated(labFileId, 1);
        for (var resultId : resultIds) {
            var lineId = firstLineIdOf(resultId);
            patchLineReal(resultId, lineId, "1.05");
            validateTechnical(resultId);
            validateBiological(resultId);
        }

        var pdf = fetchCrPdf(labFileId);

        assertThat(pdf.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pdf.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        byte[] body = pdf.getBody();
        assertThat(body).isNotEmpty();
        assertThat(body[0]).isEqualTo((byte) '%');
        assertThat(body[1]).isEqualTo((byte) 'P');

        try (var doc = PDDocument.load(body)) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            var text = new PDFTextStripper().getText(doc);
            assertThat(text).contains(labFileId);
            assertThat(text).contains(analysisCode);
            assertThat(text).contains("1.05");
            assertThat(text).contains("Compte-rendu");
            assertThat(text).contains("Résultats");
        }
    }

    @Test
    void renderedPdfShowsUpdatedLabSettingsName() throws Exception {
        // Update the lab settings before driving the rest of the flow — the assembler reads them
        // at render time.
        labSettingsApi.update(
                new LabSettingsUpdateRequest(
                        "Laboratoire Sparkcorp",
                        "12 Avenue du Test, Lomé",
                        "ACC-2026-001",
                        "Dr. KOFFI Adjoa"));

        var patientId = seedPatient();
        var disciplineId = seedDiscipline("Biochimie", "BIOCHIMIE");
        var analysisCode = "GLU_" + Ulid.generate().substring(10, 16);
        seedAnalysis(analysisCode, "Glucose", disciplineId);

        var labFileId = openLabFile(patientId, List.of(analysisCode));

        var resultIds = waitForResultsCreated(labFileId, 1);
        for (var resultId : resultIds) {
            var lineId = firstLineIdOf(resultId);
            patchLineReal(resultId, lineId, "1.05");
            validateTechnical(resultId);
            validateBiological(resultId);
        }

        var pdf = fetchCrPdf(labFileId);
        assertThat(pdf.getStatusCode()).isEqualTo(HttpStatus.OK);

        try (var doc = PDDocument.load(pdf.getBody())) {
            var text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Laboratoire Sparkcorp");
        }
    }

    @Test
    void stressTest_30Analytes_across4Paillasses_rendersMultiPagePdf() throws Exception {
        var patientId = seedPatient();
        var disciplines =
                List.of(
                        seedDiscipline("Biochimie", "BIOCHIMIE"),
                        seedDiscipline("Hématologie", "HEMATOLOGIE"),
                        seedDiscipline("Immuno-sérologie", "IMMUNO_SEROLOGIE"),
                        seedDiscipline("Microbiologie", "MICROBIOLOGIE"));

        var codes = new java.util.ArrayList<String>();
        for (int i = 0; i < 30; i++) {
            var code = "AN_" + Ulid.generate().substring(10, 18);
            seedAnalysis(code, "Analyse " + i, disciplines.get(i % disciplines.size()));
            codes.add(code);
        }

        var labFileId = openLabFile(patientId, codes);

        var resultIds = waitForResultsCreated(labFileId, codes.size());
        for (var resultId : resultIds) {
            var lineId = firstLineIdOf(resultId);
            patchLineReal(resultId, lineId, "1.23");
            validateTechnical(resultId);
            validateBiological(resultId);
        }

        var pdf = fetchCrPdf(labFileId);
        try (var doc = PDDocument.load(pdf.getBody())) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
        }
    }

    // --- helpers --------------------------------------------------------------------------------

    private String openLabFile(String patientId, List<String> analysisCodes) {
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> created =
                admin.post()
                        .uri("/api/v1/lab-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "patientId",
                                        patientId,
                                        "urgent",
                                        false,
                                        "analyses",
                                        analysisCodes))
                        .retrieve()
                        .toEntity(Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var labFileId = (String) created.getBody().get("id");
        assertThat(labFileId).isNotNull();
        return labFileId;
    }

    private List<String> waitForResultsCreated(String labFileId, int expectedSize) {
        var ref = new java.util.concurrent.atomic.AtomicReference<List<String>>();
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            var results = resultApi.findActiveByLabFile(new LabFileId(labFileId));
                            assertThat(results).hasSize(expectedSize);
                            assertThat(results)
                                    .allMatch(r -> r.state() == ResultState.DRAFT)
                                    .allMatch(r -> !r.lines().isEmpty());
                            ref.set(results.stream().map(r -> r.id().value()).toList());
                        });
        return ref.get();
    }

    private String firstLineIdOf(String resultId) {
        var results =
                resultApi.findActiveByLabFile(new LabFileId(extractLabFileIdFromResult(resultId)));
        return results.stream()
                .filter(r -> r.id().value().equals(resultId))
                .findFirst()
                .orElseThrow()
                .lines()
                .get(0)
                .id()
                .value();
    }

    private String extractLabFileIdFromResult(String resultId) {
        // Look up the result directly via the API to find its lab-file FK.
        var found =
                resultApi
                        .findById(new org.sparkcorporation.lims.result.api.ResultId(resultId))
                        .orElseThrow();
        return found.labFileId().value();
    }

    private void patchLineReal(String resultId, String lineId, String value) {
        var patched =
                technician
                        .patch()
                        .uri("/api/v1/results/" + resultId + "/lines/" + lineId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("value", Map.of("kind", "REAL", "value", value)))
                        .retrieve()
                        .toBodilessEntity();
        assertThat(patched.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void validateTechnical(String resultId) {
        var resp =
                technician
                        .post()
                        .uri("/api/v1/results/" + resultId + ":validate-technical")
                        .retrieve()
                        .toBodilessEntity();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void validateBiological(String resultId) {
        var resp =
                biologist
                        .post()
                        .uri("/api/v1/results/" + resultId + ":validate-biological")
                        .retrieve()
                        .toBodilessEntity();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<byte[]> fetchCrPdf(String labFileId) {
        return biologist
                .get()
                .uri("/api/v1/results/" + labFileId + "/pdf")
                .retrieve()
                .toEntity(byte[].class);
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

    private String seedDiscipline(String name, String prefix) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO disciplines (id, code, name, discipline_prefix, version) VALUES (?, ?,"
                        + " ?, ?, 0)",
                id,
                prefix + "_" + id.substring(10, 16),
                name,
                prefix);
        return id;
    }

    private void seedAnalysis(String code, String name, String disciplineId) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO analyses (id, code, name, kind, indiciary_value_b, label_count,"
                        + " discipline_id, result_type, version) VALUES (?, ?, ?, 'SIMPLE', 10.00,"
                        + " 1, ?, 'REAL', 0)",
                id,
                code,
                name,
                disciplineId);
    }
}
