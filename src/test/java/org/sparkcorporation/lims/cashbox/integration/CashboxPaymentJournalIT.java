package org.sparkcorporation.lims.cashbox.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashboxPaymentJournalIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");
    private static final String CASHIER_USER_ID = "01KQZ0WPVQ9EKB5AZXKWS0AYYJ";

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void cleanRows() {
        jdbc.update("DELETE FROM cashbox_journal_entries");
        jdbc.update("DELETE FROM payments");
        jdbc.update("DELETE FROM cash_register_users");
        jdbc.update("DELETE FROM cash_registers");
    }

    @Test
    void recordedPaymentLandsInCashboxJournal() {
        var patientId = seedPatient();
        var labFileId = seedOpenLabFile(patientId);
        seedIssuedInvoice(labFileId, patientId);
        var registerId = seedCashRegisterAndAssignCashier("J1", 41);

        var cashier = restClientFor(port, "CASHIER");

        @SuppressWarnings("unchecked")
        Map<String, Object> paymentResponse =
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
                                        10000,
                                        "cashRegisterId",
                                        registerId))
                        .retrieve()
                        .body(Map.class);
        String paymentId = (String) paymentResponse.get("id");

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            List<Map<String, Object>> rows =
                                    jdbc.queryForList(
                                            "SELECT operation_kind, source_id, cash_register_id,"
                                                + " lab_file_id, payment_mode, amount FROM"
                                                + " cashbox_journal_entries WHERE operation_kind ="
                                                + " 'PAYMENT' AND source_id = ?",
                                            paymentId);
                            assertThat(rows).hasSize(1);
                            assertThat(rows.get(0).get("cash_register_id")).isEqualTo(registerId);
                            assertThat(rows.get(0).get("lab_file_id")).isEqualTo(labFileId);
                            assertThat(rows.get(0).get("payment_mode")).isEqualTo("CASH");
                        });
    }

    private String seedCashRegisterAndAssignCashier(String code, int sequenceNumber) {
        var admin = restClientFor(port, "ADMIN");
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
                "INSERT INTO patients (id, lab_code, last_name, civility, sex, insured, version)"
                        + " VALUES (?, ?, 'DOE', 'MADAME', 'F', false, 0)",
                id,
                "LAB-" + id.substring(0, 10));
        return id;
    }

    private String seedOpenLabFile(String patientId) {
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

    private String seedIssuedInvoice(String labFileId, String patientId) {
        var id = Ulid.generate();
        var seq = String.format("%04d", REG_SEQ.getAndIncrement());
        var number = "INV" + LocalDate.now().format(DATE_FMT).substring(0, 2) + "-" + seq + "-001";
        jdbc.update(
                "INSERT INTO invoices (id, number, lab_file_id, issued_at, customer_kind,"
                        + " customer_id, customer_name_snapshot, home_collection_fee_snapshot,"
                        + " additional_fees_snapshot, total_amount, net_amount, created_at,"
                        + " version) VALUES (?, ?, ?, now(), 'PATIENT', ?, 'DOE', 0, 0, 10000,"
                        + " 10000, now(), 0)",
                id,
                number,
                labFileId,
                patientId);
        return id;
    }
}
