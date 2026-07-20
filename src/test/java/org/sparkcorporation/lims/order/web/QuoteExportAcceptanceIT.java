package org.sparkcorporation.lims.order.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
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
 * Acceptance IT for the quote Word export endpoint ({@code GET /api/v1/quotes/{id}/export}).
 *
 * <p>Asserts: a user with {@code quote.read} gets 200 + a valid DOCX; a user without {@code
 * quote.read} gets 403.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuoteExportAcceptanceIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private LabSettingsApi labSettingsApi;
    @Autowired private JdbcTemplate jdbc;

    private RestClient admin;
    private RestClient cashier;

    @BeforeEach
    void setUp() {
        admin = restClientFor(port, "ADMIN");
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
    void GET_returns200_withValidDocx_forAuthorisedUser() throws Exception {
        String patientId = seedPatient();
        String analysisCode = seedAnalysis();
        String quoteId = createDraftQuote(patientId, List.of(analysisCode));

        var response =
                admin.get()
                        .uri("/api/v1/quotes/{id}/export?format={fmt}", quoteId, "docx")
                        .retrieve()
                        .toEntity(byte[].class);

        assertThat(response.getHeaders().containsHeader("X-Export-Password"))
                .as("quote export must NOT include X-Export-Password header")
                .isFalse();

        byte[] docxBytes = response.getBody();
        // Verify it is a valid DOCX by opening it with POI
        try (var doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            assertThat(doc.getParagraphs()).isNotEmpty();
        }
    }

    @Test
    void GET_returns403_forUserWithoutQuoteRead() {
        String patientId = seedPatient();
        String analysisCode = seedAnalysis();
        String quoteId = createDraftQuote(patientId, List.of(analysisCode));

        assertThatThrownBy(
                        () ->
                                cashier.get()
                                        .uri(
                                                "/api/v1/quotes/{id}/export?format={fmt}",
                                                quoteId,
                                                "docx")
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(403)));
    }

    // ----- helpers --------------------------------------------------------------------------

    private byte[] fetchExport(RestClient client, String quoteId, String format) {
        return client.get()
                .uri("/api/v1/quotes/{id}/export?format={fmt}", quoteId, format)
                .retrieve()
                .body(byte[].class);
    }

    private String createDraftQuote(String patientId, List<String> analysisCodes) {
        @SuppressWarnings("unchecked")
        var resp =
                admin.post()
                        .uri("/api/v1/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("patientId", patientId, "analyses", analysisCodes))
                        .retrieve()
                        .toEntity(Map.class);
        var location = resp.getHeaders().getLocation();
        if (location == null) {
            throw new AssertionError("expected Location header on POST /api/v1/quotes");
        }
        var path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private String seedPatient() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO patients (id, lab_code, last_name, first_names, civility, sex,"
                    + " insured, version) VALUES (?, ?, 'DOE', 'Jane', 'MADAME', 'F', false, 0)",
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
