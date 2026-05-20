package org.sparkcorporation.lims.cashbox.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashboxJournalReadIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");
    private static final String CASHIER_USER_ID = "01KQZ0WPVQ9EKB5AZXKWS0AYYJ";
    private static final String SEEDED_CATEGORY_ID = "01KRXY4CP5B3C4VHZ47NJ0Z0XJ";

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void cleanRows() {
        jdbc.update("DELETE FROM cashbox_journal_entries");
        jdbc.update("DELETE FROM cashbox_expenses");
        jdbc.update("DELETE FROM payments");
        jdbc.update("DELETE FROM cash_register_users");
        jdbc.update("DELETE FROM cash_registers");
    }

    @AfterEach
    void clearPaymentsToUnblockOtherITs() {
        jdbc.update("DELETE FROM cashbox_journal_entries");
        jdbc.update("DELETE FROM payments");
    }

    @Test
    void journalReadReturnsPaidSoFarAndRemainingAcrossThreePaymentsPlusExpense() {
        var patientId = seedPatient();
        var labFileId = seedOpenLabFile(patientId);
        seedIssuedInvoice(labFileId, patientId, new BigDecimal("10000"));
        var registerId = seedCashRegisterAndAssignCashier("J6", 46);

        var cashier = restClientFor(port, "CASHIER");

        var p1Id = postPayment(cashier, labFileId, registerId, 3000);
        var p2Id = postPayment(cashier, labFileId, registerId, 4000);
        var p3Id = postPayment(cashier, labFileId, registerId, 2000);

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            Integer count =
                                    jdbc.queryForObject(
                                            "SELECT COUNT(*) FROM cashbox_journal_entries WHERE"
                                                + " operation_kind = 'PAYMENT' AND lab_file_id = ?",
                                            Integer.class,
                                            labFileId);
                            assertThat(count).isEqualTo(3);
                        });

        postExpense(cashier, SEEDED_CATEGORY_ID, registerId, 500);

        var from = Instant.now().minus(Duration.ofHours(1));
        var to = Instant.now().plus(Duration.ofHours(1));

        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                cashier.get()
                        .uri(
                                u ->
                                        u.path("/api/v1/cashbox/journal")
                                                .queryParam("from", from.toString())
                                                .queryParam("to", to.toString())
                                                .build())
                        .retrieve()
                        .body(Map.class);

        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertThat(items).hasSize(4);

        var paymentRows =
                items.stream().filter(r -> "PAYMENT".equals(r.get("operationKind"))).toList();
        assertThat(paymentRows).hasSize(3);

        assertThat(asBigDecimal(paymentRows.get(0).get("paidSoFar"))).isEqualByComparingTo("3000");
        assertThat(asBigDecimal(paymentRows.get(0).get("remaining"))).isEqualByComparingTo("7000");
        assertThat(asBigDecimal(paymentRows.get(0).get("invoiceTotal")))
                .isEqualByComparingTo("10000");

        assertThat(asBigDecimal(paymentRows.get(1).get("paidSoFar"))).isEqualByComparingTo("7000");
        assertThat(asBigDecimal(paymentRows.get(1).get("remaining"))).isEqualByComparingTo("3000");

        assertThat(asBigDecimal(paymentRows.get(2).get("paidSoFar"))).isEqualByComparingTo("9000");
        assertThat(asBigDecimal(paymentRows.get(2).get("remaining"))).isEqualByComparingTo("1000");

        var expenseRow =
                items.stream()
                        .filter(r -> "EXPENSE".equals(r.get("operationKind")))
                        .findFirst()
                        .orElseThrow();
        assertThat(expenseRow.get("invoiceTotal")).isNull();
        assertThat(expenseRow.get("paidSoFar")).isNull();
        assertThat(expenseRow.get("remaining")).isNull();

        assertThat(p1Id).isNotNull();
        assertThat(p2Id).isNotNull();
        assertThat(p3Id).isNotNull();
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
            org.springframework.web.client.RestClient client,
            String labFileId,
            String registerId,
            int amount) {
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
                                        "CASH",
                                        "amount",
                                        amount,
                                        "cashRegisterId",
                                        registerId))
                        .retrieve()
                        .body(Map.class);
        return (String) resp.get("id");
    }

    private void postExpense(
            org.springframework.web.client.RestClient client,
            String categoryId,
            String registerId,
            int amount) {
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
