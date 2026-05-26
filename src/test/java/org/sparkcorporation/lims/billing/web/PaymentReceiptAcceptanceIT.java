package org.sparkcorporation.lims.billing.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.labsettings.api.LabSettingsApi;
import org.sparkcorporation.lims.labsettings.api.LabSettingsUpdateRequest;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * End-to-end acceptance IT for the per-payment receipt PDF endpoint (spec 1.5.g). Seeds a patient +
 * an open lab-file via raw JDBC, provisions a cash-register and assigns the seeded CASHIER user via
 * the admin REST API, records a payment via {@code POST /api/v1/payments}, then asserts the {@code
 * GET /api/v1/payments/{id}/receipt.pdf} response is a valid PDF whose extracted text contains the
 * expected fields.
 *
 * <p>Mirrors the seeding pattern of {@link
 * org.sparkcorporation.lims.billing.integration.PaymentE2EIT} (lab-file/register/assignment) and
 * the LabSettings restore pattern of {@code ResultCrPdfAcceptanceIT}.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentReceiptAcceptanceIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");
    private static final String CASHIER_USER_ID = "01KQZ0WPVQ9EKB5AZXKWS0AYYJ";

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private LabSettingsApi labSettingsApi;

    @BeforeEach
    void cleanPaymentRows() {
        // payments → cashbox_journal_entries → cash_register_users → cash_registers.
        // The journal-entries table has FK references to payments, so it must be cleaned first.
        jdbc.update("DELETE FROM cashbox_journal_entries");
        jdbc.update("DELETE FROM payments");
        jdbc.update("DELETE FROM cash_register_users");
        jdbc.update("DELETE FROM cash_registers");
    }

    // Restore the Liquibase seed values so subsequent IT classes inherit a known state
    // (mirrors ResultCrPdfAcceptanceIT chunk-6 pattern).
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
    void GET_returns200_andPdfContainsExpectedText() throws Exception {
        // Use a known lab name so PDFBox can find it in the rendered text.
        labSettingsApi.update(
                new LabSettingsUpdateRequest(
                        "Laboratoire Sparkcorp",
                        "12 Avenue du Test, Lomé",
                        "ACC-2026-001",
                        "Dr. Manager"));

        var patientId = seedPatient();
        var labFileId = seedLabFile(patientId);
        var registerId = createRegisterAndAssignCashier("R1", 21);

        var cashier = restClientFor(port, "CASHIER");

        // Record a payment via the existing cashier endpoint.
        @SuppressWarnings("unchecked")
        Map<String, Object> paymentBody =
                cashier.post()
                        .uri("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "labFileId",
                                        labFileId,
                                        "mode",
                                        "CASH",
                                        "amount",
                                        5000,
                                        "cashRegisterId",
                                        registerId))
                        .retrieve()
                        .body(Map.class);
        assertThat(paymentBody).isNotNull();
        var paymentId = (String) paymentBody.get("id");
        assertThat(paymentId).isNotBlank();

        // Fetch the receipt PDF.
        ResponseEntity<byte[]> pdf =
                cashier.get()
                        .uri("/api/v1/payments/{id}/receipt.pdf", paymentId)
                        .retrieve()
                        .toEntity(byte[].class);

        assertThat(pdf.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pdf.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        var disposition = pdf.getHeaders().getFirst("Content-Disposition");
        assertThat(disposition).isNotNull();
        assertThat(disposition).startsWith("inline;");

        byte[] body = pdf.getBody();
        assertThat(body).isNotEmpty();
        assertThat(new String(body, 0, 4)).isEqualTo("%PDF");

        String text;
        try (var doc = PDDocument.load(body)) {
            text = new PDFTextStripper().getText(doc);
        }

        assertThat(text).contains("Laboratoire Sparkcorp");
        assertThat(text).contains("REÇU DE PAIEMENT");
        assertThat(text).contains("Reçu N° " + paymentId);
        assertThat(text).contains("Espèces");
        assertThat(text)
                .contains(
                        NumberFormat.getInstance(Locale.FRENCH).format(new BigDecimal("5000"))
                                + " F CFA");
        // Spec assertions: patient last name + cashier display name (seeded "Test Cashier"
        // in 202605061002-seed-iam-test-users.yaml).
        assertThat(text).contains("DOE");
        assertThat(text).contains("Test Cashier");
    }

    @Test
    void GET_returns404_forUnknownId() {
        // CASHIER has invoice.read so the request reaches the controller and the assembler.
        var cashier = restClientFor(port, "CASHIER");
        // Valid 26-char Crockford Base32 ULID (alphabet excludes I, L, O, U) that no row holds.
        var unknown = "01HZX0000000000000NTFND0ZZ";

        assertThatThrownBy(
                        () ->
                                cashier.get()
                                        .uri("/api/v1/payments/{id}/receipt.pdf", unknown)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex -> {
                            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                            assertThat(ex.getResponseHeaders().getContentType())
                                    .isNotNull()
                                    .satisfies(
                                            ct ->
                                                    assertThat(ct.toString())
                                                            .startsWith(
                                                                    MediaType
                                                                            .APPLICATION_PROBLEM_JSON_VALUE));
                            assertThat(ex.getResponseBodyAsString())
                                    .contains("\"title\":\"payment not found\"");
                        });
    }

    // --- helpers --------------------------------------------------------------------------------

    /**
     * Provisions a cash-register via the admin endpoint and assigns the seeded CASHIER user to it.
     * Mirrors the helper in {@link
     * org.sparkcorporation.lims.billing.integration.PaymentE2EIT#createRegisterAndAssignCashier}.
     */
    private String createRegisterAndAssignCashier(String code, int sequenceNumber) {
        RestClient admin = restClientFor(port, "ADMIN");

        @SuppressWarnings("unchecked")
        Map<String, Object> created =
                admin.post()
                        .uri("/api/v1/cash-registers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "code",
                                        code,
                                        "label",
                                        "Caisse " + sequenceNumber,
                                        "sequenceNumber",
                                        sequenceNumber))
                        .retrieve()
                        .body(Map.class);
        var registerId = (String) created.get("id");

        admin.put()
                .uri("/api/v1/cash-registers/{id}/users", registerId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("userIds", List.of(CASHIER_USER_ID)))
                .retrieve()
                .toBodilessEntity();
        return registerId;
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

    private String seedLabFile(String patientId) {
        var id = Ulid.generate();
        var seq = String.format("%04d", REG_SEQ.getAndIncrement());
        var regNum = "LF-" + LocalDate.now().format(DATE_FMT) + "-" + seq;
        jdbc.update(
                "INSERT INTO lab_files (id, registration_number, patient_id, urgent, status,"
                        + " created_at, updated_at, version) VALUES (?, ?, ?, false, 'OPEN',"
                        + " now(), now(), 0)",
                id,
                regNum,
                patientId);
        return id;
    }
}
