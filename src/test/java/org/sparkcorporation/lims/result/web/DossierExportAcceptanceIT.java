package org.sparkcorporation.lims.result.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.labsettings.api.LabSettingsApi;
import org.sparkcorporation.lims.labsettings.api.LabSettingsUpdateRequest;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.api.ResultApi;
import org.sparkcorporation.lims.result.api.ResultState;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Acceptance IT for the dossier office export endpoint ({@code GET /api/v1/lab-files/{id}/export}).
 *
 * <p>Asserts:
 *
 * <ul>
 *   <li>A user with {@code lab-file.read} gets 200 + valid OOXML encrypted with the password in
 *       {@code X-Export-Password} — for both DOCX and XLSX.
 *   <li>A user without {@code lab-file.read} gets 403.
 *   <li>A dossier that is not yet biologically validated returns 409.
 * </ul>
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DossierExportAcceptanceIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ResultApi resultApi;
    @Autowired private LabSettingsApi labSettingsApi;

    private RestClient admin;
    private RestClient technician;
    private RestClient biologist;
    private RestClient cashier;

    @BeforeEach
    void setUp() {
        admin = restClientFor(port, "ADMIN");
        technician = restClientFor(port, "TECHNICIAN");
        biologist = restClientFor(port, "BIOLOGIST");
        cashier = restClientFor(port, "CASHIER");
        labSettingsApi.update(
                new LabSettingsUpdateRequest(
                        "Lab Export Test",
                        "12 Avenue du Test, Lomé",
                        "ACC-2026-001",
                        "Dr. Manager"));
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
    void docx_export_decrypts_with_header_password() throws Exception {
        String labFileId = seedValidatedDossier();

        ResponseEntity<byte[]> response = fetchExport(biologist, labFileId, "docx");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));

        String password = response.getHeaders().getFirst("X-Export-Password");
        assertThat(password).isNotBlank();

        byte[] body = response.getBody();
        assertThat(body).isNotEmpty();

        // Prove the body decrypts with the header password and fails with a wrong password
        assertThat(opensWith(body, password))
                .as("body should decrypt with X-Export-Password value")
                .isTrue();
        assertThat(opensWith(body, "WRONGPASSWORD"))
                .as("body should NOT decrypt with a wrong password")
                .isFalse();
    }

    @Test
    void xlsx_export_decrypts_with_header_password() throws Exception {
        String labFileId = seedValidatedDossier();

        ResponseEntity<byte[]> response = fetchExport(biologist, labFileId, "xlsx");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        String password = response.getHeaders().getFirst("X-Export-Password");
        assertThat(password).isNotBlank();

        byte[] body = response.getBody();
        assertThat(body).isNotEmpty();

        assertThat(opensWith(body, password))
                .as("body should decrypt with X-Export-Password value")
                .isTrue();
        assertThat(opensWith(body, "WRONGPASSWORD"))
                .as("body should NOT decrypt with a wrong password")
                .isFalse();
    }

    @Test
    void export_returns_403_for_user_without_lab_file_read() {
        String patientId = seedPatient();
        String analysisCode = seedAnalysis();
        String labFileId = openLabFile(patientId, List.of(analysisCode));

        assertThatThrownBy(
                        () ->
                                cashier.get()
                                        .uri("/api/v1/lab-files/{id}/export?format=docx", labFileId)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(403)));
    }

    @Test
    void export_returns_409_for_non_validated_dossier() {
        String patientId = seedPatient();
        String analysisCode = seedAnalysis();
        String labFileId = openLabFile(patientId, List.of(analysisCode));

        // Wait for eager result creation but do NOT validate — leave in DRAFT
        waitForResultsCreated(labFileId, 1);

        assertThatThrownBy(
                        () ->
                                biologist
                                        .get()
                                        .uri("/api/v1/lab-files/{id}/export?format=docx", labFileId)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(409)));
    }

    // --- helpers ---------------------------------------------------------------------------

    private String seedValidatedDossier() {
        String patientId = seedPatient();
        String analysisCode = seedAnalysis();
        String labFileId = openLabFile(patientId, List.of(analysisCode));

        List<String> resultIds = waitForResultsCreated(labFileId, 1);
        for (String resultId : resultIds) {
            String lineId = firstLineIdOf(resultId);
            patchLineReal(resultId, lineId, "1.05");
            validateTechnical(resultId);
            validateBiological(resultId);
        }
        return labFileId;
    }

    private ResponseEntity<byte[]> fetchExport(RestClient client, String labFileId, String format) {
        return client.get()
                .uri("/api/v1/lab-files/{id}/export?format={fmt}", labFileId, format)
                .retrieve()
                .toEntity(byte[].class);
    }

    private static boolean opensWith(byte[] encrypted, String password) throws Exception {
        try (var fs = new POIFSFileSystem(new ByteArrayInputStream(encrypted))) {
            var dec = Decryptor.getInstance(new EncryptionInfo(fs));
            return dec.verifyPassword(password);
        }
    }

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
        var labFileId = extractLabFileIdFromResult(resultId);
        var results = resultApi.findActiveByLabFile(new LabFileId(labFileId));
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
        return resultApi
                .findById(new org.sparkcorporation.lims.result.api.ResultId(resultId))
                .orElseThrow()
                .labFileId()
                .value();
    }

    private void patchLineReal(String resultId, String lineId, String value) {
        var resp =
                technician
                        .patch()
                        .uri("/api/v1/results/" + resultId + "/lines/" + lineId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("value", Map.of("kind", "REAL", "value", value)))
                        .retrieve()
                        .toBodilessEntity();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
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

    private String seedPatient() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO patients (id, lab_code, last_name, civility, sex, insured, version)"
                        + " VALUES (?, ?, 'DOE', 'MADAME', 'F', false, 0)",
                id,
                "LAB-" + id.substring(0, 10));
        return id;
    }

    private String seedAnalysis() {
        var disciplineId = Ulid.generate();
        jdbc.update(
                "INSERT INTO disciplines (id, code, name, discipline_prefix, version) VALUES (?,"
                        + " ?, 'Biochimie', 'BIOCHIMIE', 0)",
                disciplineId,
                "BIOCHEM-" + disciplineId.substring(10, 16));
        var analysisId = Ulid.generate();
        var code = "GLU_" + analysisId.substring(10, 16);
        jdbc.update(
                "INSERT INTO analyses (id, code, name, kind, indiciary_value_b, label_count,"
                        + " discipline_id, result_type, version) VALUES (?, ?, 'Glucose', 'SIMPLE',"
                        + " 10.00, 1, ?, 'REAL', 0)",
                analysisId,
                code,
                disciplineId);
        return code;
    }
}
