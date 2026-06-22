package org.sparkcorporation.lims.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.order.infra.SampleDestructionQueueProjection;
import org.sparkcorporation.lims.order.infra.SampleRepository;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Repository IT for {@link SampleRepository#findDestructionDue}: only samples past their computed
 * destruction deadline and not yet destroyed appear in the queue. Seeds three samples directly via
 * SQL — overdue-not-destroyed (appears), overdue-but-destroyed (excluded), not-yet-due (excluded).
 */
@Tag("integration")
@SpringBootTest
class SampleDestructionQueueRepositoryIT extends IntegrationTestBase {

    @Autowired private SampleRepository sampleRepository;
    @Autowired private JdbcTemplate jdbc;

    private static final Instant NOW = Instant.parse("2026-07-01T09:00:00Z");

    @Test
    void returns_only_overdue_undestroyed_samples() {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        var analysisCode = "GLU_" + uniq();
        var analysisId = seedAnalysis(analysisCode, disciplineId);
        var recipientId = seedRecipient();
        var labFileId = seedLabFile(patientId);

        var overdueId =
                seedSample(labFileId, analysisId, recipientId, NOW.minus(5, ChronoUnit.DAYS), null);
        // overdue but already destroyed → excluded
        var destroyedId =
                seedSample(
                        labFileId,
                        analysisId,
                        recipientId,
                        NOW.minus(5, ChronoUnit.DAYS),
                        NOW.minus(1, ChronoUnit.DAYS));
        // not yet due → excluded
        var notDueId =
                seedSample(labFileId, analysisId, recipientId, NOW.plus(5, ChronoUnit.DAYS), null);

        // The queue is global, so other ITs' overdue samples may share the shared Testcontainers
        // DB.
        // Scope the assertions to the three rows this test seeded rather than the absolute count.
        var rows = sampleRepository.findDestructionDue(NOW, PageRequest.of(0, 1000)).getContent();
        assertThat(rows)
                .extracting(SampleDestructionQueueProjection::getSampleId)
                .contains(overdueId)
                .doesNotContain(destroyedId, notDueId);

        SampleDestructionQueueProjection row =
                rows.stream()
                        .filter(r -> overdueId.equals(r.getSampleId()))
                        .findFirst()
                        .orElseThrow();
        assertThat(row.getAnalysisCode()).isEqualTo(analysisCode);
        assertThat(row.getPortoir()).isEqualTo("P12");
        assertThat(row.getDestructionDueAt()).isEqualTo(NOW.minus(5, ChronoUnit.DAYS));
        assertThat(row.getDaysOverdue()).isEqualTo(5);
        assertThat(row.getLabFileId()).isEqualTo(labFileId);
        assertThat(row.getLabFileReference()).isNotBlank();
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
        jdbc.update(
                "INSERT INTO lab_files (id, registration_number, patient_id, urgent, status,"
                    + " created_at, updated_at, version) VALUES (?, ?, ?, false, 'OPEN', ?, ?, 0)",
                id,
                "LF-260701-" + String.format("%04d", REG_SEQ.getAndIncrement()),
                patientId,
                java.sql.Timestamp.from(NOW),
                java.sql.Timestamp.from(NOW));
        return id;
    }

    private String seedSample(
            String labFileId,
            String analysisId,
            String recipientId,
            Instant destructionDueAt,
            Instant destroyedAt) {
        var id = Ulid.generate();
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
                java.sql.Timestamp.from(destructionDueAt.minus(30, ChronoUnit.DAYS)),
                java.sql.Timestamp.from(destructionDueAt.minus(30, ChronoUnit.DAYS)),
                java.sql.Timestamp.from(destructionDueAt),
                destroyedAt == null ? null : java.sql.Timestamp.from(destroyedAt));
        return id;
    }
}
