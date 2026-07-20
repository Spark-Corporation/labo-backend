package org.sparkcorporation.lims.stats.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Acceptance IT for the statistics XLSX export endpoint ({@code GET /api/v1/stats/export}).
 *
 * <p>Asserts:
 *
 * <ul>
 *   <li>A user with {@code stats.read} gets 200 + a valid six-sheet XLSX workbook.
 *   <li>A user without {@code stats.read} gets 403.
 *   <li>{@code from > to} → 400 (read model throws {@code IllegalArgumentException}).
 * </ul>
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StatsExportAcceptanceIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void cleanStats() {
        // Remove any leftover lab-file graph rows to avoid FK conflicts across tests.
        jdbc.update("DELETE FROM antibiogram_readings");
        jdbc.update("DELETE FROM result_lines");
        jdbc.update("DELETE FROM payments");
        jdbc.update("DELETE FROM invoice_lines");
        jdbc.update("DELETE FROM invoices");
        jdbc.update("DELETE FROM results");
        jdbc.update("DELETE FROM analysis_requests");
        jdbc.update("DELETE FROM samples");
        jdbc.update("DELETE FROM dossier_comments");
        jdbc.update("DELETE FROM quote_lines");
        jdbc.update("DELETE FROM quotes");
        jdbc.update("DELETE FROM lab_files");
        jdbc.update("DELETE FROM patients WHERE last_name = 'DOE'");
    }

    @Test
    void GET_returns_200_with_valid_six_sheet_xlsx_for_authorised_user() throws Exception {
        LocalDate today = LocalDate.now();
        Instant inRange =
                today.atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .plus(2, ChronoUnit.HOURS);
        String patientId = seedPatient("M", inRange);
        seedLabFile(patientId, inRange);

        RestClient admin = restClientFor(port, "ADMIN");

        var response =
                admin.get()
                        .uri(
                                "/api/v1/stats/export?format={fmt}&from={f}&to={t}",
                                "xlsx",
                                today.toString(),
                                today.toString())
                        .retrieve()
                        .toEntity(byte[].class);

        assertThat(response.getHeaders().containsHeader("X-Export-Password"))
                .as("stats export must NOT include X-Export-Password header")
                .isFalse();

        byte[] body = response.getBody();
        assertThat(body).isNotNull();

        try (var wb = new XSSFWorkbook(new ByteArrayInputStream(body))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(6);
            assertThat(wb.getSheet("Activité")).isNotNull();
            assertThat(wb.getSheet("Par discipline")).isNotNull();
            assertThat(wb.getSheet("Par prescripteur")).isNotNull();
            assertThat(wb.getSheet("Analyses")).isNotNull();
            assertThat(wb.getSheet("Antibiogrammes")).isNotNull();
            assertThat(wb.getSheet("Patients")).isNotNull();
        }
    }

    @Test
    void GET_returns_403_for_user_without_stats_read() {
        // TECHNICIAN is NOT granted stats.read (only ADMIN/LAB_MANAGER/BIOLOGIST/ACCOUNTANT are)
        RestClient technician = restClientFor(port, "TECHNICIAN");

        assertThatThrownBy(
                        () ->
                                technician
                                        .get()
                                        .uri(
                                                "/api/v1/stats/export?format={fmt}&from={f}&to={t}",
                                                "xlsx",
                                                "2026-05-01",
                                                "2026-05-31")
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(403)));
    }

    @Test
    void GET_returns_400_when_from_is_after_to() {
        RestClient admin = restClientFor(port, "ADMIN");

        assertThatThrownBy(
                        () ->
                                admin.get()
                                        .uri(
                                                "/api/v1/stats/export?format={fmt}&from={f}&to={t}",
                                                "xlsx",
                                                "2026-05-31",
                                                "2026-05-01")
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(400)));
    }

    // ---- minimal seed helpers -----------------------------------------------------------------

    private String seedPatient(String sex, Instant createdAt) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO patients (id, lab_code, last_name, civility, sex, insured,"
                        + " created_at, version) VALUES (?, ?, 'DOE', 'MADAME', ?, false, ?, 0)",
                id,
                "LAB-" + id.substring(0, 10),
                sex,
                java.sql.Timestamp.from(createdAt));
        return id;
    }

    private void seedLabFile(String patientId, Instant createdAt) {
        var id = Ulid.generate();
        var seq = String.format("%04d", REG_SEQ.getAndIncrement());
        var regNum = "LF-" + LocalDate.now().format(DATE_FMT) + "-" + seq;
        jdbc.update(
                "INSERT INTO lab_files (id, registration_number, patient_id, urgent, status,"
                        + " created_at, updated_at, deleted_at, version)"
                        + " VALUES (?, ?, ?, false, 'OPEN', ?, ?, NULL, 0)",
                id,
                regNum,
                patientId,
                java.sql.Timestamp.from(createdAt),
                java.sql.Timestamp.from(createdAt));
    }
}
