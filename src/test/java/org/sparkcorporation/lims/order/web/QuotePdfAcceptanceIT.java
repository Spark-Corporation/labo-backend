package org.sparkcorporation.lims.order.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
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
 * End-to-end acceptance IT for the quote PDF endpoint (spec 1.5.h). Seeds a patient, a discipline,
 * and an analysis via raw JDBC; creates a quote via REST; fetches the DRAFT-state PDF; locks the
 * quote; fetches the LOCKED-state PDF. Asserts PDFBox-extracted text against expected fixtures.
 *
 * <p>Uses ADMIN throughout because {@code quote.read} is currently granted only to ADMIN per the
 * IAM seed (RECEPTIONIST has create/send/lock but not read).
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuotePdfAcceptanceIT extends IntegrationTestBase {

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
    void GET_returns200_forDraftQuote_andPdfShowsBrouillonBanner() throws Exception {
        String patientId = seedPatient();
        String analysisCode = seedAnalysis();
        String quoteId = createDraftQuote(patientId, List.of(analysisCode));

        var pdfBytes = fetchPdf(quoteId);
        var text = extractText(pdfBytes);

        assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");
        assertThat(text).contains("Lab Sparkcorp");
        assertThat(text).contains("BROUILLON");
        assertThat(text).contains("non contractuel");
        assertThat(text).contains("À déterminer");
        assertThat(text).contains("DOE");
    }

    @Test
    @SuppressWarnings("unchecked")
    void GET_returns200_forLockedQuote_andPdfContainsExpectedText() throws Exception {
        String patientId = seedPatient();
        String analysisCode = seedAnalysis();
        String quoteId = createDraftQuote(patientId, List.of(analysisCode));
        Map<String, Object> locked =
                admin.post().uri("/api/v1/quotes/" + quoteId + ":lock").retrieve().body(Map.class);
        assertThat((String) locked.get("status")).isEqualTo("LOCKED");
        var totalAmount = new BigDecimal(locked.get("totalAmount").toString());
        assertThat(totalAmount).isGreaterThan(BigDecimal.ZERO);

        var pdfBytes = fetchPdf(quoteId);
        var text = extractText(pdfBytes);

        assertThat(text).contains("Lab Sparkcorp");
        assertThat(text).contains("DEVIS");
        assertThat(text).doesNotContain("BROUILLON");
        assertThat(text).doesNotContain("non contractuel");
        assertThat(text).contains("DOE");
        assertThat(text)
                .contains(NumberFormat.getInstance(Locale.FRENCH).format(totalAmount) + " F CFA");
        assertThat(text).contains(analysisCode); // analysis code appears in the lines table
    }

    @Test
    void GET_returns404_forUnknownId() {
        var unknown = "01HZX0000000000000NTFND0ZZ";

        assertThatThrownBy(
                        () ->
                                admin.get()
                                        .uri("/api/v1/quotes/{id}/pdf", unknown)
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

    // ----- helpers --------------------------------------------------------------------------

    private byte[] fetchPdf(String quoteId) {
        return admin.get().uri("/api/v1/quotes/{id}/pdf", quoteId).retrieve().body(byte[].class);
    }

    private static String extractText(byte[] pdf) throws Exception {
        try (var doc = PDDocument.load(new ByteArrayInputStream(pdf))) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private String createDraftQuote(String patientId, List<String> analysisCodes) {
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

    /**
     * Mirrors the JDBC seed pattern in {@code PaymentReceiptAcceptanceIT.seedPatient}. Returns the
     * ULID of the inserted patient row.
     */
    private String seedPatient() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO patients (id, lab_code, last_name, first_names, civility, sex,"
                    + " insured, version) VALUES (?, ?, 'DOE', 'Jane', 'MADAME', 'F', false, 0)",
                id,
                "LAB-" + id.substring(0, 10));
        return id;
    }

    /**
     * Mirrors {@code QuoteCrudIT.seedDiscipline} + {@code QuoteCrudIT.seedAnalysis}. Returns the
     * generated analysis code.
     */
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
