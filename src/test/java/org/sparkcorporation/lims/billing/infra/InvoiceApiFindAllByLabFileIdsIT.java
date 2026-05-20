package org.sparkcorporation.lims.billing.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.billing.api.InvoiceApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.shared.id.Ulid;

@Tag("integration")
@SpringBootTest
class InvoiceApiFindAllByLabFileIdsIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    @Autowired private InvoiceApi invoiceApi;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void returnsOnlyRequestedKeys_absentKeysMeanNoInvoice() {
        String patientId = seedPatient(jdbcTemplate);
        LabFileId labFileWithInvoice = seedOpenLabFile(jdbcTemplate, patientId);
        LabFileId labFileWithoutInvoice = seedOpenLabFile(jdbcTemplate, patientId);
        seedIssuedInvoice(jdbcTemplate, labFileWithInvoice, patientId);

        var result =
                invoiceApi.findAllByLabFileIds(Set.of(labFileWithInvoice, labFileWithoutInvoice));

        assertThat(result).containsOnlyKeys(labFileWithInvoice);
        assertThat(result.get(labFileWithInvoice).labFileId()).isEqualTo(labFileWithInvoice);
    }

    @Test
    void emptyInputReturnsEmptyMap() {
        assertThat(invoiceApi.findAllByLabFileIds(Set.of())).isEmpty();
    }

    private static String seedPatient(JdbcTemplate jdbc) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO patients (id, lab_code, last_name, civility, sex, insured, version)"
                        + " VALUES (?, ?, 'DOE', 'MADAME', 'F', false, 0)",
                id,
                "LAB-" + id.substring(0, 10));
        return id;
    }

    private static LabFileId seedOpenLabFile(JdbcTemplate jdbc, String patientId) {
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
        return new LabFileId(id);
    }

    private static void seedIssuedInvoice(
            JdbcTemplate jdbc, LabFileId labFileId, String patientId) {
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
                labFileId.value(),
                patientId);
    }
}
