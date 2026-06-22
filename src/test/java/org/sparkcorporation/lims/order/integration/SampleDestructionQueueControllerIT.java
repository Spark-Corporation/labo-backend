package org.sparkcorporation.lims.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.HttpClientErrorException;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Acceptance IT for {@code GET /api/v1/samples/queues/destruction-due} (CDC Onglet « Conservation
 * »). A {@code lab-file.read} user sees overdue-undestroyed samples (not-yet-due excluded); a role
 * lacking {@code lab-file.read} (CASHIER) gets 403.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SampleDestructionQueueControllerIT extends IntegrationTestBase {

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;

    private static final Instant LONG_OVERDUE = Instant.parse("2020-01-01T00:00:00Z");
    private static final Instant FAR_FUTURE = Instant.parse("2099-01-01T00:00:00Z");

    @Test
    void lab_file_read_user_sees_overdue_samples_only() {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        var analysisId = seedAnalysis("GLU_" + uniq(), disciplineId);
        var recipientId = seedRecipient();
        var labFileId = seedLabFile(patientId);
        var overdueId = seedSample(labFileId, analysisId, recipientId, LONG_OVERDUE, null);
        var notDueId = seedSample(labFileId, analysisId, recipientId, FAR_FUTURE, null);

        var admin = restClientFor(port, "ADMIN");
        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                admin.get()
                        .uri("/api/v1/samples/queues/destruction-due?page=0&size=200")
                        .retrieve()
                        .body(Map.class);

        @SuppressWarnings("unchecked")
        var content = (List<Map<String, Object>>) body.get("content");
        assertThat(content).anySatisfy(row -> assertThat(row.get("sampleId")).isEqualTo(overdueId));
        assertThat(content).noneSatisfy(row -> assertThat(row.get("sampleId")).isEqualTo(notDueId));
        var overdueRow =
                content.stream()
                        .filter(r -> overdueId.equals(r.get("sampleId")))
                        .findFirst()
                        .orElseThrow();
        assertThat(overdueRow.get("portoir")).isEqualTo("P12");
        assertThat((Integer) overdueRow.get("daysOverdue")).isPositive();
        assertThat(overdueRow.get("labFileReference")).isNotNull();
    }

    @Test
    void role_without_lab_file_read_is_forbidden() {
        var cashier = restClientFor(port, "CASHIER");
        try {
            cashier.get()
                    .uri("/api/v1/samples/queues/destruction-due")
                    .retrieve()
                    .toBodilessEntity();
            throw new AssertionError("expected 403 FORBIDDEN");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    private static String uniq() {
        return Ulid.generate().substring(10, 16);
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

    private String seedDiscipline() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO disciplines (id, code, name, discipline_prefix, version) VALUES (?,"
                        + " ?, 'Biochimie', 'BIOCHIMIE', 0)",
                id,
                "BIOCHEM-" + uniq());
        return id;
    }

    private String seedAnalysis(String code, String disciplineId) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO analyses (id, code, name, kind, indiciary_value_b, label_count,"
                        + " discipline_id, result_type, version) VALUES (?, ?, 'Glucose', 'SIMPLE',"
                        + " 10.00, 1, ?, 'REAL', 0)",
                id,
                code,
                disciplineId);
        return id;
    }

    private String seedRecipient() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO recipients (id, code, name, version) VALUES (?, ?, 'Tube EDTA', 0)",
                id,
                "TUBE-" + uniq());
        return id;
    }

    private String seedLabFile(String patientId) {
        var id = Ulid.generate();
        var ts = java.sql.Timestamp.from(LONG_OVERDUE);
        jdbc.update(
                "INSERT INTO lab_files (id, registration_number, patient_id, urgent, status,"
                    + " created_at, updated_at, version) VALUES (?, ?, ?, false, 'OPEN', ?, ?, 0)",
                id,
                "LF-200101-" + String.format("%04d", REG_SEQ.getAndIncrement()),
                patientId,
                ts,
                ts);
        return id;
    }

    private String seedSample(
            String labFileId,
            String analysisId,
            String recipientId,
            Instant destructionDueAt,
            Instant destroyedAt) {
        var id = Ulid.generate();
        var start = java.sql.Timestamp.from(destructionDueAt.minusSeconds(30L * 86400));
        jdbc.update(
                "INSERT INTO samples (id, sample_type, lab_file_id, daily_id, analysis_id,"
                    + " recipient_id, collected_at, conservation_start, conservation_days, portoir,"
                    + " destruction_due_at, destroyed_at, version) VALUES (?, 'IN_LAB', ?, ?, ?, ?,"
                    + " ?, ?, 30, 'P12', ?, ?, 0)",
                id,
                labFileId,
                // VO-valid daily_id (^[1-5]\d{6}-\d{3}$); REG_SEQ keeps it globally unique and the
                // "00…" date block can never collide with an app-generated (real-date) daily_id.
                "5" + String.format("%06d", REG_SEQ.getAndIncrement()) + "-000",
                analysisId,
                recipientId,
                start,
                start,
                java.sql.Timestamp.from(destructionDueAt),
                destroyedAt == null ? null : java.sql.Timestamp.from(destroyedAt));
        return id;
    }
}
