package org.sparkcorporation.lims.cashbox.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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

import org.sparkcorporation.lims.IntegrationTestBase;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashboxExpenseIT extends IntegrationTestBase {

    private static final String SEEDED_CATEGORY_ID = "01KRXY4CP5B3C4VHZ47NJ0Z0XJ"; // UTILITIES

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void cleanCashboxRows() {
        jdbc.update("DELETE FROM cashbox_journal_entries");
        jdbc.update("DELETE FROM cashbox_expenses");
        jdbc.update("DELETE FROM cash_register_users");
        jdbc.update("DELETE FROM cash_registers");
    }

    @Test
    void recordedExpenseLandsInJournal() {
        var admin = restClientFor(port, "ADMIN");
        @SuppressWarnings("unchecked")
        Map<String, Object> register =
                admin.post()
                        .uri("/api/v1/cash-registers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("code", "C1", "label", "Caisse C1", "sequenceNumber", 21))
                        .retrieve()
                        .body(Map.class);
        var registerId = (String) register.get("id");

        var cashier = restClientFor(port, "CASHIER");

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

        assertThat(created.get("mode")).isEqualTo("CASH");
        assertThat(created.get("categoryId")).isEqualTo(SEEDED_CATEGORY_ID);
        assertThat(created.get("cashRegisterId")).isEqualTo(registerId);
        var expenseId = (String) created.get("id");

        Integer expenseRows =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM cashbox_expenses WHERE id = ? AND category_id = ? AND"
                                + " mode = 'CASH'",
                        Integer.class,
                        expenseId,
                        SEEDED_CATEGORY_ID);
        assertThat(expenseRows).isEqualTo(1);

        BigDecimal amount =
                jdbc.queryForObject(
                        "SELECT amount FROM cashbox_expenses WHERE id = ?",
                        BigDecimal.class,
                        expenseId);
        assertThat(amount).isEqualByComparingTo("12500");

        var journalRow =
                jdbc.queryForMap(
                        "SELECT operation_kind, source_id, payment_mode, lab_file_id,"
                                + " cash_register_id, amount FROM cashbox_journal_entries WHERE"
                                + " source_id = ?",
                        expenseId);
        assertThat(journalRow.get("operation_kind")).isEqualTo("EXPENSE");
        assertThat(journalRow.get("source_id")).isEqualTo(expenseId);
        assertThat(journalRow.get("payment_mode")).isEqualTo("CASH");
        assertThat(journalRow.get("lab_file_id")).isNull();
        assertThat(journalRow.get("cash_register_id")).isEqualTo(registerId);
        assertThat((BigDecimal) journalRow.get("amount")).isEqualByComparingTo("12500");

        @SuppressWarnings("unchecked")
        Map<String, Object> listResp =
                cashier.get().uri("/api/v1/cashbox/expenses").retrieve().body(Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) listResp.get("items");
        assertThat(items).anyMatch(row -> expenseId.equals(row.get("id")));
    }

    @Test
    void postByBiologistReturns403() {
        var admin = restClientFor(port, "ADMIN");
        @SuppressWarnings("unchecked")
        Map<String, Object> register =
                admin.post()
                        .uri("/api/v1/cash-registers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("code", "C2", "label", "Caisse C2", "sequenceNumber", 22))
                        .retrieve()
                        .body(Map.class);
        var registerId = (String) register.get("id");

        var biologist = restClientFor(port, "BIOLOGIST");

        var ex =
                org.junit.jupiter.api.Assertions.assertThrows(
                        HttpClientErrorException.Forbidden.class,
                        () ->
                                biologist
                                        .post()
                                        .uri("/api/v1/cashbox/expenses")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(
                                                Map.of(
                                                        "categoryId",
                                                        SEEDED_CATEGORY_ID,
                                                        "mode",
                                                        "CASH",
                                                        "amount",
                                                        100,
                                                        "cashRegisterId",
                                                        registerId))
                                        .retrieve()
                                        .toBodilessEntity());
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
