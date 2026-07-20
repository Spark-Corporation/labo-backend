package org.sparkcorporation.lims.cashbox.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashboxAttachmentIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");
    private static final String SEEDED_CATEGORY_ID = "01KRXY4CP5B3C4VHZ47NJ0Z0XJ"; // UTILITIES
    private static final byte[] RECEIPT = "%PDF-1.7 receipt".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] RECEIPT2 =
            "%PDF-1.7 receipt v2".getBytes(StandardCharsets.US_ASCII);

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void cleanCashboxRows() {
        jdbc.update("DELETE FROM attachments");
        jdbc.update("DELETE FROM cashbox_journal_entries");
        jdbc.update("DELETE FROM cashbox_expenses");
        jdbc.update("DELETE FROM cashbox_home_collection_fees");
        jdbc.update("DELETE FROM cash_register_users");
        jdbc.update("DELETE FROM cash_registers");
    }

    @Test
    void expenseScanRoundTripAndOverwrite() {
        var cashier = restClientFor(port, "CASHIER");
        var registerId = seedRegister("E1", 41);
        var expenseId = recordExpense(cashier, registerId);

        // upload #1: links justification_scan_uri to the stored blob's URI
        @SuppressWarnings("unchecked")
        Map<String, Object> first =
                cashier.post()
                        .uri("/api/v1/cashbox/expenses/{id}/attachments", expenseId)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(multipart("receipt.pdf", RECEIPT))
                        .retrieve()
                        .body(Map.class);
        var firstUri = (String) first.get("storageUri");

        var scanUri =
                jdbc.queryForObject(
                        "SELECT justification_scan_uri FROM cashbox_expenses WHERE id = ?",
                        String.class,
                        expenseId);
        assertThat(scanUri).isNotNull().isEqualTo(firstUri);

        var attType =
                jdbc.queryForObject(
                        "SELECT attachable_type FROM attachments WHERE id = ?",
                        String.class,
                        (String) first.get("id"));
        assertThat(attType).isEqualTo("EXPENSE");

        // upload #2: overwrites the URI; both attachment rows remain live
        @SuppressWarnings("unchecked")
        Map<String, Object> second =
                cashier.post()
                        .uri("/api/v1/cashbox/expenses/{id}/attachments", expenseId)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(multipart("receipt-v2.pdf", RECEIPT2))
                        .retrieve()
                        .body(Map.class);
        var secondUri = (String) second.get("storageUri");

        var afterOverwrite =
                jdbc.queryForObject(
                        "SELECT justification_scan_uri FROM cashbox_expenses WHERE id = ?",
                        String.class,
                        expenseId);
        assertThat(afterOverwrite).isEqualTo(secondUri).isNotEqualTo(firstUri);

        Integer expenseAttachments =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM attachments WHERE attachable_type = 'EXPENSE' AND"
                                + " attachable_id = ?",
                        Integer.class,
                        expenseId);
        assertThat(expenseAttachments).isEqualTo(2);
    }

    @Test
    void feeScanRoundTripListDownloadAnd404() {
        var cashier = restClientFor(port, "CASHIER");
        var registerId = seedRegister("F1", 51);
        var feeId = recordFee(cashier, registerId);

        // upload links receipt_scan_uri
        @SuppressWarnings("unchecked")
        Map<String, Object> created =
                cashier.post()
                        .uri("/api/v1/cashbox/home-collection-fees/{id}/attachments", feeId)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(multipart("fee-receipt.pdf", RECEIPT))
                        .retrieve()
                        .body(Map.class);
        var attachmentId = (String) created.get("id");
        var feeUri =
                jdbc.queryForObject(
                        "SELECT receipt_scan_uri FROM cashbox_home_collection_fees WHERE id = ?",
                        String.class,
                        feeId);
        assertThat(feeUri).isNotNull().isEqualTo(created.get("storageUri"));

        // list
        List<?> listed =
                cashier.get()
                        .uri("/api/v1/cashbox/home-collection-fees/{id}/attachments", feeId)
                        .retrieve()
                        .body(List.class);
        assertThat(listed).hasSize(1);

        // download streams the original bytes with the detected type
        var download =
                cashier.get()
                        .uri(
                                "/api/v1/cashbox/home-collection-fees/{id}/attachments/{att}",
                                feeId,
                                attachmentId)
                        .retrieve()
                        .toEntity(byte[].class);
        assertThat(download.getBody()).containsExactly(RECEIPT);
        assertThat(download.getHeaders().getContentType()).hasToString("application/pdf");

        // download on an unknown fee id → 404
        assertThatThrownBy(
                        () ->
                                cashier.get()
                                        .uri(
                                                "/api/v1/cashbox/home-collection-fees/{id}/attachments/{att}",
                                                Ulid.generate(),
                                                attachmentId)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void uploadByBiologistReturns403() {
        var cashier = restClientFor(port, "CASHIER");
        var registerId = seedRegister("E2", 42);
        var expenseId = recordExpense(cashier, registerId);

        var biologist = restClientFor(port, "BIOLOGIST");
        assertThatThrownBy(
                        () ->
                                biologist
                                        .post()
                                        .uri("/api/v1/cashbox/expenses/{id}/attachments", expenseId)
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(multipart("receipt.pdf", RECEIPT))
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(403)));
    }

    @Test
    void uploadToUnknownExpenseReturns404() {
        var cashier = restClientFor(port, "CASHIER");

        assertThatThrownBy(
                        () ->
                                cashier.post()
                                        .uri(
                                                "/api/v1/cashbox/expenses/{id}/attachments",
                                                Ulid.generate())
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(multipart("receipt.pdf", RECEIPT))
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(404));
    }

    // ----- helpers --------------------------------------------------------------------------

    private String seedRegister(String code, int sequenceNumber) {
        var admin = restClientFor(port, "ADMIN");
        @SuppressWarnings("unchecked")
        Map<String, Object> register =
                admin.post()
                        .uri("/api/v1/cash-registers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "code",
                                        code,
                                        "label",
                                        "Caisse " + code,
                                        "sequenceNumber",
                                        sequenceNumber))
                        .retrieve()
                        .body(Map.class);
        return (String) register.get("id");
    }

    private String recordExpense(RestClient cashier, String registerId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> created =
                cashier.post()
                        .uri("/api/v1/cashbox/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "categoryId",
                                        SEEDED_CATEGORY_ID,
                                        "mode",
                                        "CASH",
                                        "amount",
                                        12500,
                                        "description",
                                        "Office water bottles",
                                        "cashRegisterId",
                                        registerId))
                        .retrieve()
                        .body(Map.class);
        return (String) created.get("id");
    }

    private String recordFee(RestClient cashier, String registerId) {
        var patientId = seedPatient();
        var labFileId = seedLabFile(patientId);
        @SuppressWarnings("unchecked")
        Map<String, Object> created =
                cashier.post()
                        .uri("/api/v1/cashbox/home-collection-fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "patientId",
                                        patientId,
                                        "labFileId",
                                        labFileId,
                                        "distanceKm",
                                        5.0,
                                        "amount",
                                        2500,
                                        "cashRegisterId",
                                        registerId))
                        .retrieve()
                        .body(Map.class);
        return (String) created.get("id");
    }

    private static LinkedMultiValueMap<String, Object> multipart(String filename, byte[] bytes) {
        var parts = new LinkedMultiValueMap<String, Object>();
        parts.add(
                "file",
                new ByteArrayResource(bytes) {
                    @Override
                    public String getFilename() {
                        return filename;
                    }
                });
        return parts;
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
