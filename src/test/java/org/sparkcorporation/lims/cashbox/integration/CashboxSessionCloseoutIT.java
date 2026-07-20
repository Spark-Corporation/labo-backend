package org.sparkcorporation.lims.cashbox.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashboxSessionCloseoutIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");
    private static final String CASHIER_USER_ID = "01KQZ0WPVQ9EKB5AZXKWS0AYYJ";
    private static final String SEEDED_CATEGORY_ID = "01KRXY4CP5B3C4VHZ47NJ0Z0XJ";

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void cleanRows() {
        jdbc.update("DELETE FROM cashbox_sessions");
        jdbc.update("DELETE FROM cashbox_journal_entries");
        jdbc.update("DELETE FROM cashbox_expenses");
        jdbc.update("DELETE FROM cashbox_home_collection_fees");
        jdbc.update("DELETE FROM payments");
        jdbc.update("DELETE FROM cash_register_users");
        jdbc.update("DELETE FROM cash_registers");
        jdbc.update("DELETE FROM invoices");
        // lab_files has incoming FKs from result/order children. Drain those before deleting
        // lab_files so prior ITs that left residual rows do not break this test's cleanup.
        jdbc.update("DELETE FROM antibiogram_readings");
        jdbc.update("DELETE FROM result_lines");
        jdbc.update("DELETE FROM results");
        jdbc.update("DELETE FROM dossier_comments");
        jdbc.update("DELETE FROM samples");
        jdbc.update("DELETE FROM analysis_requests");
        jdbc.update("DELETE FROM lab_files");
    }

    @AfterEach
    void cleanupForOtherITs() {
        jdbc.update("DELETE FROM cashbox_sessions");
        jdbc.update("DELETE FROM cashbox_journal_entries");
        jdbc.update("DELETE FROM cashbox_expenses");
        jdbc.update("DELETE FROM cashbox_home_collection_fees");
        jdbc.update("DELETE FROM payments");
        jdbc.update("DELETE FROM cash_register_users");
        jdbc.update("DELETE FROM cash_registers");
        jdbc.update("DELETE FROM invoices");
        // lab_files has incoming FKs from result/order children. Drain those before deleting
        // lab_files so prior ITs that left residual rows do not break this test's cleanup.
        jdbc.update("DELETE FROM antibiogram_readings");
        jdbc.update("DELETE FROM result_lines");
        jdbc.update("DELETE FROM results");
        jdbc.update("DELETE FROM dossier_comments");
        jdbc.update("DELETE FROM samples");
        jdbc.update("DELETE FROM analysis_requests");
        jdbc.update("DELETE FROM lab_files");
    }

    @Test
    void closeoutAggregatesPaymentsMinusExpensesPlusHomeCollections() {
        var patientId = seedPatient();
        var labFileId = seedOpenLabFile(patientId);
        seedIssuedInvoice(labFileId, patientId, new BigDecimal("20000"));
        var registerId = seedCashRegisterAndAssignCashier("S1", 51);
        var date = LocalDate.now(ZoneOffset.UTC);

        var cashier = restClientFor(port, "CASHIER");

        postPayment(cashier, labFileId, registerId, 10000, "CASH");
        postPayment(cashier, labFileId, registerId, 5000, "CHEQUE");
        postExpense(cashier, SEEDED_CATEGORY_ID, registerId, 2000);
        postHomeCollectionFee(cashier, patientId, labFileId, registerId, 3000);

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            Integer count =
                                    jdbc.queryForObject(
                                            "SELECT COUNT(*) FROM cashbox_journal_entries"
                                                    + " WHERE cash_register_id = ?",
                                            Integer.class,
                                            registerId);
                            assertThat(count).isEqualTo(4);
                        });

        @SuppressWarnings("unchecked")
        Map<String, Object> closed =
                cashier.post()
                        .uri("/api/v1/cashbox/sessions/{r}/{d}:close", registerId, date.toString())
                        .retrieve()
                        .body(Map.class);

        assertThat(closed.get("status")).isEqualTo("CLOSED");
        assertThat(asBigDecimal(closed.get("cashTotal"))).isEqualByComparingTo("10000");
        assertThat(asBigDecimal(closed.get("chequeTotal"))).isEqualByComparingTo("5000");
        assertThat(asBigDecimal(closed.get("expensesTotal"))).isEqualByComparingTo("2000");
        assertThat(asBigDecimal(closed.get("homeCollectionTotal"))).isEqualByComparingTo("3000");
        assertThat(asBigDecimal(closed.get("grandTotal"))).isEqualByComparingTo("16000");
    }

    @Test
    void closeoutBucketsJournalEntriesByUtcDate() {
        var registerId = seedCashRegisterAndAssignCashier("S4", 54);
        var date = LocalDate.of(2026, 1, 15);
        // 00:30Z and 23:30Z on the session date: each lands on a different calendar day in
        // any non-UTC session timezone (west resp. east of UTC), so a session-timezone date
        // cast in the totals query drops one of them while UTC bucketing keeps both.
        seedCashJournalEntry(registerId, 7000, date.atTime(0, 30).toInstant(ZoneOffset.UTC));
        seedCashJournalEntry(registerId, 3000, date.atTime(23, 30).toInstant(ZoneOffset.UTC));

        var cashier = restClientFor(port, "CASHIER");

        @SuppressWarnings("unchecked")
        Map<String, Object> closed =
                cashier.post()
                        .uri("/api/v1/cashbox/sessions/{r}/{d}:close", registerId, date.toString())
                        .retrieve()
                        .body(Map.class);

        assertThat(closed.get("status")).isEqualTo("CLOSED");
        assertThat(asBigDecimal(closed.get("cashTotal"))).isEqualByComparingTo("10000");
    }

    @Test
    void reopenAsReceptionistReturns403() {
        var registerId = seedCashRegisterAndAssignReceptionist("S2", 52);
        var date = LocalDate.now(ZoneOffset.UTC);

        var receptionist = restClientFor(port, "RECEPTIONIST");
        receptionist
                .post()
                .uri("/api/v1/cashbox/sessions/{r}/{d}:close", registerId, date.toString())
                .retrieve()
                .toBodilessEntity();

        var ex =
                org.junit.jupiter.api.Assertions.assertThrows(
                        HttpClientErrorException.Forbidden.class,
                        () ->
                                receptionist
                                        .post()
                                        .uri(
                                                "/api/v1/cashbox/sessions/{r}/{d}:reopen",
                                                registerId,
                                                date.toString())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(Map.of("reason", "typo"))
                                        .retrieve()
                                        .toBodilessEntity());
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void reopenAsLabManagerReturns200() {
        var registerId = seedCashRegisterAndAssignReceptionist("S3", 53);
        var date = LocalDate.now(ZoneOffset.UTC);

        var receptionist = restClientFor(port, "RECEPTIONIST");
        receptionist
                .post()
                .uri("/api/v1/cashbox/sessions/{r}/{d}:close", registerId, date.toString())
                .retrieve()
                .toBodilessEntity();

        var labManager = restClientFor(port, "LAB-MANAGER");

        @SuppressWarnings("unchecked")
        Map<String, Object> reopened =
                labManager
                        .post()
                        .uri("/api/v1/cashbox/sessions/{r}/{d}:reopen", registerId, date.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("reason", "typo"))
                        .retrieve()
                        .body(Map.class);

        assertThat(reopened.get("status")).isEqualTo("OPEN");
        assertThat(reopened.get("reopenReason")).isEqualTo("typo");
        assertThat(reopened.get("reopenedBy")).isNotNull();
        assertThat(reopened.get("reopenedAt")).isNotNull();
    }

    private static BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        return new BigDecimal(value.toString());
    }

    private String postPayment(
            RestClient client, String labFileId, String registerId, int amount, String mode) {
        @SuppressWarnings("unchecked")
        Map<String, Object> resp =
                client.post()
                        .uri("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "labFileId",
                                        labFileId,
                                        "mode",
                                        mode,
                                        "amount",
                                        amount,
                                        "cashRegisterId",
                                        registerId))
                        .retrieve()
                        .body(Map.class);
        return (String) resp.get("id");
    }

    private void postExpense(RestClient client, String categoryId, String registerId, int amount) {
        client.post()
                .uri("/api/v1/cashbox/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        Map.of(
                                "categoryId",
                                categoryId,
                                "mode",
                                "CASH",
                                "amount",
                                amount,
                                "cashRegisterId",
                                registerId))
                .retrieve()
                .toBodilessEntity();
    }

    private void postHomeCollectionFee(
            RestClient client, String patientId, String labFileId, String registerId, int amount) {
        client.post()
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
                                amount,
                                "cashRegisterId",
                                registerId))
                .retrieve()
                .toBodilessEntity();
    }

    private void seedCashJournalEntry(String registerId, int amount, Instant occurredAt) {
        jdbc.update(
                "INSERT INTO cashbox_journal_entries (id, operation_kind, source_id,"
                        + " cash_register_id, user_id, payment_mode, amount, occurred_at)"
                        + " VALUES (?, 'PAYMENT', ?, ?, ?, 'CASH', ?, ?)",
                Ulid.generate(),
                Ulid.generate(),
                registerId,
                CASHIER_USER_ID,
                amount,
                OffsetDateTime.ofInstant(occurredAt, ZoneOffset.UTC));
    }

    private String seedCashRegisterAndAssignCashier(String code, int sequenceNumber) {
        return seedCashRegisterAndAssignUser(code, sequenceNumber, CASHIER_USER_ID);
    }

    private String seedCashRegisterAndAssignReceptionist(String code, int sequenceNumber) {
        var receptionistId = lookupUserId("receptionist@test.local");
        return seedCashRegisterAndAssignUser(code, sequenceNumber, receptionistId);
    }

    private String seedCashRegisterAndAssignUser(String code, int sequenceNumber, String userId) {
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
                .body(Map.of("userIds", List.of(userId)))
                .retrieve()
                .toBodilessEntity();
        return registerId;
    }

    private String lookupUserId(String email) {
        return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", String.class, email);
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

    private String seedIssuedInvoice(String labFileId, String patientId, BigDecimal total) {
        var id = Ulid.generate();
        var seq = String.format("%03d", REG_SEQ.getAndIncrement());
        var dateSegment = LocalDate.now().format(DATE_FMT);
        var number =
                "INV" + dateSegment.substring(0, 2) + "-" + dateSegment.substring(2, 6) + "-" + seq;
        jdbc.update(
                "INSERT INTO invoices (id, number, lab_file_id, issued_at, customer_kind,"
                    + " customer_id, customer_name_snapshot, home_collection_fee_snapshot,"
                    + " additional_fees_snapshot, total_amount, net_amount, created_at, version)"
                    + " VALUES (?, ?, ?, now(), 'PATIENT', ?, 'DOE', 0, 0, ?, ?, now(), 0)",
                id,
                number,
                labFileId,
                patientId,
                total,
                total);
        return id;
    }
}
