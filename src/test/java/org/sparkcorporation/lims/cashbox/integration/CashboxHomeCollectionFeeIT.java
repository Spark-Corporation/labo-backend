package org.sparkcorporation.lims.cashbox.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import org.sparkcorporation.lims.shared.id.Ulid;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashboxHomeCollectionFeeIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void cleanCashboxRows() {
        jdbc.update("DELETE FROM cashbox_journal_entries");
        jdbc.update("DELETE FROM cashbox_home_collection_fees");
        jdbc.update("DELETE FROM cash_register_users");
        jdbc.update("DELETE FROM cash_registers");
    }

    @Test
    void feeRecordedSyncsToLabFile() {
        var admin = restClientFor(port, "ADMIN");
        @SuppressWarnings("unchecked")
        Map<String, Object> register =
                admin.post()
                        .uri("/api/v1/cash-registers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("code", "H1", "label", "Caisse H1", "sequenceNumber", 31))
                        .retrieve()
                        .body(Map.class);
        var registerId = (String) register.get("id");

        String patientId = seedPatient(jdbc);
        String labFileId = seedLabFile(jdbc, "OPEN", patientId);

        var cashier = restClientFor(port, "CASHIER");

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

        assertThat(created.get("labFileId")).isEqualTo(labFileId);
        assertThat(created.get("cashRegisterId")).isEqualTo(registerId);
        var feeId = (String) created.get("id");

        Integer feeRows =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM cashbox_home_collection_fees WHERE id = ?",
                        Integer.class,
                        feeId);
        assertThat(feeRows).isEqualTo(1);

        BigDecimal labFileFee =
                jdbc.queryForObject(
                        "SELECT home_collection_fee FROM lab_files WHERE id = ?",
                        BigDecimal.class,
                        labFileId);
        assertThat(labFileFee).isEqualByComparingTo("2500");

        Integer journalRows =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM cashbox_journal_entries WHERE source_id = ? AND"
                                + " operation_kind = 'HOME_COLLECTION_FEE'",
                        Integer.class,
                        feeId);
        assertThat(journalRows).isEqualTo(1);
    }

    @Test
    void feeNotPersistedWhenLabFileIsClosed() {
        var admin = restClientFor(port, "ADMIN");
        @SuppressWarnings("unchecked")
        Map<String, Object> register =
                admin.post()
                        .uri("/api/v1/cash-registers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("code", "H2", "label", "Caisse H2", "sequenceNumber", 32))
                        .retrieve()
                        .body(Map.class);
        var registerId = (String) register.get("id");

        String patientId = seedPatient(jdbc);
        String labFileId = seedLabFile(jdbc, "BIOLOGICALLY_VALIDATED", patientId);

        var cashier = restClientFor(port, "CASHIER");

        var ex =
                org.junit.jupiter.api.Assertions.assertThrows(
                        HttpClientErrorException.class,
                        () ->
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
                                        .toBodilessEntity());
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        Integer feeRows =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM cashbox_home_collection_fees WHERE lab_file_id = ?",
                        Integer.class,
                        labFileId);
        assertThat(feeRows).isEqualTo(0);

        Integer journalRows =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM cashbox_journal_entries WHERE lab_file_id = ? AND"
                                + " operation_kind = 'HOME_COLLECTION_FEE'",
                        Integer.class,
                        labFileId);
        assertThat(journalRows).isEqualTo(0);
    }

    @Test
    void postByBiologistReturns403() {
        var admin = restClientFor(port, "ADMIN");
        @SuppressWarnings("unchecked")
        Map<String, Object> register =
                admin.post()
                        .uri("/api/v1/cash-registers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("code", "H3", "label", "Caisse H3", "sequenceNumber", 33))
                        .retrieve()
                        .body(Map.class);
        var registerId = (String) register.get("id");

        String patientId = seedPatient(jdbc);
        String labFileId = seedLabFile(jdbc, "OPEN", patientId);

        var biologist = restClientFor(port, "BIOLOGIST");

        var ex =
                org.junit.jupiter.api.Assertions.assertThrows(
                        HttpClientErrorException.Forbidden.class,
                        () ->
                                biologist
                                        .post()
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
                                        .toBodilessEntity());
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private static String seedLabFile(JdbcTemplate jdbc, String status, String patientId) {
        var id = Ulid.generate();
        var seq = String.format("%04d", REG_SEQ.getAndIncrement());
        var regNum = "LF-" + LocalDate.now().format(DATE_FMT) + "-" + seq;
        jdbc.update(
                "INSERT INTO lab_files (id, registration_number, patient_id, urgent, status,"
                        + " created_at, updated_at, version) VALUES (?, ?, ?, false, ?,"
                        + " now(), now(), 0)",
                id,
                regNum,
                patientId,
                status);
        return id;
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
}
