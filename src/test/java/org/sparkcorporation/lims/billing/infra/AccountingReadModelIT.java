package org.sparkcorporation.lims.billing.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Integration test for the {@link AccountingReadModel} native projections — drives the three SQL
 * queries against a real PostgreSQL container with hand-seeded billing fixtures. Docker-blocked
 * locally; runs in CI per the same convention as the other {@code *IT} infra tests.
 */
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AccountingReadModel.class)
class AccountingReadModelIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    @Autowired private JdbcTemplate jdbc;
    @Autowired private AccountingReadModel readModel;

    @BeforeEach
    void cleanBillingRows() {
        jdbc.update("DELETE FROM payments");
        jdbc.update("DELETE FROM invoices");
    }

    @Test
    void daily_returns_one_row_per_lab_file_active_on_date() {
        LocalDate today = LocalDate.now();
        Instant todayInstant =
                today.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().plusSeconds(3600);
        var patientId = seedPatient();
        var registerId = seedCashRegister("R1", 41);
        var lab1 = seedLabFile(patientId);
        var lab2 = seedLabFile(patientId);

        // lab1: invoice 5000, payment 3000 today
        seedInvoice(lab1, patientId, "5000.00", todayInstant);
        seedPayment(lab1, registerId, "CASH", "3000.00", todayInstant);

        // lab2: invoice 8000 today, no payment
        seedInvoice(lab2, patientId, "8000.00", todayInstant);

        var rows = readModel.daily(today);

        assertThat(rows).hasSize(2);
        var lab1Row =
                rows.stream().filter(r -> r.labFileId().equals(lab1)).findFirst().orElseThrow();
        assertThat(lab1Row.totalAmount()).isEqualByComparingTo("5000.00");
        assertThat(lab1Row.paidAmount()).isEqualByComparingTo("3000.00");
        assertThat(lab1Row.remainingAmount()).isEqualByComparingTo("2000.00");

        var lab2Row =
                rows.stream().filter(r -> r.labFileId().equals(lab2)).findFirst().orElseThrow();
        assertThat(lab2Row.totalAmount()).isEqualByComparingTo("8000.00");
        assertThat(lab2Row.paidAmount()).isEqualByComparingTo("0");
        assertThat(lab2Row.remainingAmount()).isEqualByComparingTo("8000.00");
    }

    @Test
    void by_payment_mode_groups_payments_by_mode_within_window() {
        Instant now = Instant.now();
        var patientId = seedPatient();
        var labFileId = seedLabFile(patientId);
        var registerId = seedCashRegister("R2", 42);

        seedPayment(labFileId, registerId, "CASH", "1000.00", now.minusSeconds(30));
        seedPayment(labFileId, registerId, "CASH", "2500.00", now.minusSeconds(20));
        seedPayment(labFileId, registerId, "CHEQUE", "4000.00", now.minusSeconds(10));
        // outside the window — must be excluded
        seedPayment(labFileId, registerId, "CASH", "999.00", now.minusSeconds(3600));

        var rows = readModel.byPaymentMode(now.minusSeconds(60), now.plusSeconds(60));

        assertThat(rows).hasSize(2);
        var cashRow = rows.stream().filter(r -> r.mode().equals("CASH")).findFirst().orElseThrow();
        assertThat(cashRow.totalAmount()).isEqualByComparingTo("3500.00");
        assertThat(cashRow.paymentCount()).isEqualTo(2);

        var chequeRow =
                rows.stream().filter(r -> r.mode().equals("CHEQUE")).findFirst().orElseThrow();
        assertThat(chequeRow.totalAmount()).isEqualByComparingTo("4000.00");
        assertThat(chequeRow.paymentCount()).isEqualTo(1);
    }

    @Test
    void outstanding_splits_unpaid_and_refunds_owed_excluding_fully_paid() {
        Instant now = Instant.now();
        var patientId = seedPatient();
        var registerId = seedCashRegister("R3", 43);

        // unpaid: paid (3000) < total (5000)
        var unpaidLab = seedLabFile(patientId);
        seedInvoice(unpaidLab, patientId, "5000.00", now);
        seedPayment(unpaidLab, registerId, "CASH", "3000.00", now);

        // refund-owed: paid (6000) > total (4000)
        var overpaidLab = seedLabFile(patientId);
        seedInvoice(overpaidLab, patientId, "4000.00", now);
        seedPayment(overpaidLab, registerId, "CASH", "6000.00", now);

        // fully paid — neither bucket
        var paidLab = seedLabFile(patientId);
        seedInvoice(paidLab, patientId, "2000.00", now);
        seedPayment(paidLab, registerId, "CASH", "2000.00", now);

        var result = readModel.outstanding();

        assertThat(result.unpaid()).hasSize(1);
        assertThat(result.unpaid().get(0).labFileId()).isEqualTo(unpaidLab);
        assertThat(result.unpaid().get(0).remainingAmount()).isEqualByComparingTo("2000.00");

        assertThat(result.refundsOwed()).hasSize(1);
        assertThat(result.refundsOwed().get(0).labFileId()).isEqualTo(overpaidLab);
        // total (4000) - paid (6000) = -2000
        assertThat(result.refundsOwed().get(0).remainingAmount()).isEqualByComparingTo("2000.00");
    }

    // --- helpers -------------------------------------------------------------------------------

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

    private void seedInvoice(String labFileId, String patientId, String totalAmount, Instant at) {
        var id = Ulid.generate();
        var number = "ACR26-1305-" + id.substring(20, 26).replaceAll("[^0-9]", "2");
        jdbc.update(
                "INSERT INTO invoices (id, number, lab_file_id, issued_at, customer_kind,"
                        + " customer_id, customer_name_snapshot, home_collection_fee_snapshot,"
                        + " additional_fees_snapshot, total_amount, insurance_coverage_amount,"
                        + " discount_amount, net_amount, created_at, version) VALUES (?, ?, ?, ?,"
                        + " 'PATIENT', ?, 'DOE', 0, 0, ?, 0, 0, ?, ?, 0)",
                id,
                number,
                labFileId,
                Timestamp.from(at),
                patientId,
                new BigDecimal(totalAmount),
                new BigDecimal(totalAmount),
                Timestamp.from(at));
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
                new BigDecimal(amount),
                registerId,
                Ulid.generate(),
                Timestamp.from(recordedAt));
    }
}
