package org.sparkcorporation.lims.billing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Acceptance IT for the {@code /api/v1/accounting/*} read-side surface (spec §VIII, plan chunk 8).
 * Drives the live REST endpoints with an ADMIN token (carries {@code accounting.read}). Seeds the
 * underlying lab-files, invoices, and payments via raw JDBC for tight control over the temporal
 * predicates exercised by the native SQL projections.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountingControllerIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void cleanBillingRows() {
        // payments must go before invoices (payments.invoice_id FK)
        jdbc.update("DELETE FROM payments");
        jdbc.update("DELETE FROM invoices");
    }

    @Test
    void daily_recap_groups_invoices_and_payments_by_date() {
        LocalDate today = LocalDate.now();
        seedTwoInvoicesAndOnePaymentOnDate(today);

        var http = restClientFor(port, "ADMIN");
        @SuppressWarnings("unchecked")
        Map<String, Object> response =
                http.get()
                        .uri("/api/v1/accounting/daily?date={date}", today.toString())
                        .retrieve()
                        .body(Map.class);

        assertThat(response).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> files = (List<Map<String, Object>>) response.get("files");
        assertThat(files).hasSize(2);

        @SuppressWarnings("unchecked")
        Map<String, Object> totals = (Map<String, Object>) response.get("totals");
        assertThat(totals).containsKeys("totalAmount", "paidAmount", "remainingAmount");
    }

    @Test
    void by_payment_mode_groups_payments_by_mode_within_window() {
        Instant now = Instant.now();
        var patientId = seedPatient();
        var labFileId = seedLabFile(patientId);
        var registerId = seedCashRegister("D2", 21);
        seedPayment(labFileId, registerId, "CASH", "1000", now.minusSeconds(30));
        seedPayment(labFileId, registerId, "CASH", "2500", now.minusSeconds(20));
        seedPayment(labFileId, registerId, "CHEQUE", "4000", now.minusSeconds(10));

        var http = restClientFor(port, "ADMIN");
        Instant from = now.minusSeconds(60);
        Instant to = now.plusSeconds(60);
        @SuppressWarnings("unchecked")
        Map<String, Object> response =
                http.get()
                        .uri(
                                "/api/v1/accounting/by-payment-mode?from={from}&to={to}",
                                from.toString(),
                                to.toString())
                        .retrieve()
                        .body(Map.class);

        assertThat(response).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) response.get("rows");
        assertThat(rows).hasSize(2);
        @SuppressWarnings("unchecked")
        Map<String, Object> totals = (Map<String, Object>) response.get("totals");
        assertThat(totals).containsKeys("totalAmount", "paidAmount", "remainingAmount");
    }

    @Test
    void outstanding_splits_into_unpaid_and_refunds_owed() {
        Instant now = Instant.now();
        var patientId = seedPatient();
        var registerId = seedCashRegister("D3", 22);

        // unpaid invoice: paid (3000) < total (5000)
        var unpaidLab = seedLabFile(patientId);
        seedInvoice(unpaidLab, patientId, "5000", now);
        seedPayment(unpaidLab, registerId, "CASH", "3000", now);

        // refund-owed invoice: paid (6000) > total (4000)
        var overpaidLab = seedLabFile(patientId);
        seedInvoice(overpaidLab, patientId, "4000", now);
        seedPayment(overpaidLab, registerId, "CASH", "6000", now);

        // fully paid invoice — must NOT appear in either bucket
        var paidLab = seedLabFile(patientId);
        seedInvoice(paidLab, patientId, "2000", now);
        seedPayment(paidLab, registerId, "CASH", "2000", now);

        var http = restClientFor(port, "ADMIN");
        @SuppressWarnings("unchecked")
        Map<String, Object> response =
                http.get().uri("/api/v1/accounting/outstanding").retrieve().body(Map.class);

        assertThat(response).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> unpaid = (List<Map<String, Object>>) response.get("unpaid");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> refundsOwed =
                (List<Map<String, Object>>) response.get("refundsOwed");

        assertThat(unpaid).hasSize(1);
        assertThat((String) unpaid.get(0).get("labFileId")).isEqualTo(unpaidLab);
        assertThat(refundsOwed).hasSize(1);
        assertThat((String) refundsOwed.get(0).get("labFileId")).isEqualTo(overpaidLab);
    }

    @Test
    void daily_returns_400_when_date_param_missing() {
        var http = restClientFor(port, "ADMIN");
        var status =
                http.get()
                        .uri("/api/v1/accounting/daily")
                        .exchange((req, res) -> res.getStatusCode());
        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // --- helpers -------------------------------------------------------------------------------

    private void seedTwoInvoicesAndOnePaymentOnDate(LocalDate date) {
        var instant = date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().plusSeconds(3600);
        var patientId = seedPatient();
        var lab1 = seedLabFile(patientId);
        var lab2 = seedLabFile(patientId);
        seedInvoice(lab1, patientId, "5000", instant);
        seedInvoice(lab2, patientId, "8000", instant);
        var registerId = seedCashRegister("D1", 31);
        seedPayment(lab1, registerId, "CASH", "5000", instant);
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

    private static final AtomicInteger INVOICE_SEQ = new AtomicInteger();

    private void seedInvoice(String labFileId, String patientId, String totalAmount, Instant at) {
        var id = Ulid.generate();
        var number = "ACC26-0513-" + String.format("%06d", INVOICE_SEQ.incrementAndGet());
        jdbc.update(
                "INSERT INTO invoices (id, number, lab_file_id, issued_at, customer_kind,"
                        + " customer_id, customer_name_snapshot, home_collection_fee_snapshot,"
                        + " additional_fees_snapshot, total_amount, insurance_coverage_amount,"
                        + " discount_amount, net_amount, created_at, version) VALUES (?, ?, ?, ?,"
                        + " 'PATIENT', ?, 'DOE', 0, 0, ?, 0, 0, ?, ?, 0)",
                id,
                number,
                labFileId,
                java.sql.Timestamp.from(at),
                patientId,
                new java.math.BigDecimal(totalAmount),
                new java.math.BigDecimal(totalAmount),
                java.sql.Timestamp.from(at));
    }

    private String seedCashRegister(String code, int sequenceNumber) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO cash_registers (id, code, label, sequence_number, created_at,"
                        + " updated_at, version) VALUES (?, ?, ?, ?, now(), now(), 0)",
                id,
                code,
                "Caisse " + sequenceNumber,
                sequenceNumber);
        return id;
    }

    private void seedPayment(
            String labFileId, String registerId, String mode, String amount, Instant recordedAt) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO payments (id, lab_file_id, mode, amount, cash_register_id,"
                        + " recorded_by_user_id, recorded_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id,
                labFileId,
                mode,
                new java.math.BigDecimal(amount),
                registerId,
                Ulid.generate(),
                java.sql.Timestamp.from(recordedAt));
    }
}
