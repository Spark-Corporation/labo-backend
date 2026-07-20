package org.sparkcorporation.lims.result.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * JPA-slice round-trip for {@link DossierRepository#findOverdue}. Covers spec §8.2 matrix:
 * earliest-deadline canary, bio-validated exclusion, null-delay exclusion, deadline-not-yet-passed
 * exclusion, soft-delete exclusion, ordering, zero-analysis-request exclusion, tie-breaking, and
 * soft-deleted-result-row tolerance.
 *
 * <p>Each test seeds its own lab files and filters assertions to those IDs — the singleton Postgres
 * container is shared across all ITs, and per-test {@code @Transactional} rollback from
 * {@code @DataJpaTest} handles cleanup, so tests don't truncate shared tables and don't rely on
 * empty-table assumptions.
 *
 * <p>Docker-blocked locally (Testcontainers PostgreSQL via {@link IntegrationTestBase}); runs in
 * CI.
 */
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaDossierRepositoryOverdueIT.AuditingTestConfig.class)
@DirtiesContext
class JpaDossierRepositoryOverdueIT extends IntegrationTestBase {

    @TestConfiguration
    @EnableJpaAuditing
    static class AuditingTestConfig {
        @Bean
        org.springframework.data.domain.AuditorAware<String> auditorAware() {
            return Optional::empty;
        }
    }

    @Autowired private DossierRepository repository;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void findOverdue_returns_lab_file_with_earliest_deadline_first() {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        var analysisShortDelay = seedAnalysis("GLY", "Glycemie", disciplineId, 0, 1); // 1 hour
        var analysisLongDelay = seedAnalysis("CRE", "Creatinine", disciplineId, 1, 0); // 1 day

        var labA = seedLabFile(patientId, Instant.now().minus(3, ChronoUnit.HOURS));
        seedAnalysisRequest(labA, analysisShortDelay); // overdue by ~2h

        var labB = seedLabFile(patientId, Instant.now().minus(30, ChronoUnit.HOURS));
        seedAnalysisRequest(labB, analysisLongDelay); // overdue by ~6h

        var rows =
                onlySeeded(
                        repository.findOverdue(PageRequest.of(0, 200)).getContent(),
                        Set.of(labA, labB));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getLabFileId()).isEqualTo(labB); // more overdue first
        assertThat(rows.get(0).getHoursPastDeadline()).isEqualTo(6);
        assertThat(rows.get(0).getAnalysisCode()).startsWith("CRE");
        assertThat(rows.get(0).getOverdueAnalysisCount()).isEqualTo(1);

        assertThat(rows.get(1).getLabFileId()).isEqualTo(labA);
        assertThat(rows.get(1).getHoursPastDeadline()).isEqualTo(2);
    }

    @Test
    void findOverdue_excludes_lab_file_when_all_overdue_analyses_are_bio_validated() {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        var analysis = seedAnalysis("UR", "Uree", disciplineId, 0, 1);
        var lab = seedLabFile(patientId, Instant.now().minus(3, ChronoUnit.HOURS));
        var ar = seedAnalysisRequest(lab, analysis);
        seedResult(lab, ar, "BIOLOGICAL_VALIDATED");

        assertThat(
                        onlySeeded(
                                repository.findOverdue(PageRequest.of(0, 200)).getContent(),
                                Set.of(lab)))
                .isEmpty();
    }

    @Test
    void findOverdue_picks_earliest_deadline_when_lab_file_has_multiple_overdue_analyses() {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        var earliest = seedAnalysis("EAR", "Earliest", disciplineId, 0, 1); // 1 hour
        var later = seedAnalysis("LAT", "Later", disciplineId, 1, 0); // 1 day
        var lab = seedLabFile(patientId, Instant.now().minus(30, ChronoUnit.HOURS));
        seedAnalysisRequest(lab, earliest);
        seedAnalysisRequest(lab, later);

        var rows =
                onlySeeded(
                        repository.findOverdue(PageRequest.of(0, 200)).getContent(), Set.of(lab));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getAnalysisCode()).startsWith("EAR");
        assertThat(rows.get(0).getOverdueAnalysisCount()).isEqualTo(2);
    }

    @Test
    void findOverdue_excludes_analysis_with_null_delay_config() {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        var noDelay = seedAnalysis("ND", "No Delay", disciplineId, null, null); // both null
        var lab = seedLabFile(patientId, Instant.now().minus(72, ChronoUnit.HOURS));
        seedAnalysisRequest(lab, noDelay);

        assertThat(
                        onlySeeded(
                                repository.findOverdue(PageRequest.of(0, 200)).getContent(),
                                Set.of(lab)))
                .isEmpty();
    }

    @Test
    void findOverdue_excludes_lab_file_whose_deadline_has_not_passed() {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        var future = seedAnalysis("FUT", "Future", disciplineId, 7, 0); // 7 days
        var lab = seedLabFile(patientId, Instant.now().minus(1, ChronoUnit.HOURS));
        seedAnalysisRequest(lab, future);

        assertThat(
                        onlySeeded(
                                repository.findOverdue(PageRequest.of(0, 200)).getContent(),
                                Set.of(lab)))
                .isEmpty();
    }

    @Test
    void findOverdue_excludes_soft_deleted_lab_file() {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        var a = seedAnalysis("SD", "Soft Deleted", disciplineId, 0, 1);
        var lab = seedLabFile(patientId, Instant.now().minus(3, ChronoUnit.HOURS));
        seedAnalysisRequest(lab, a);
        jdbc.update("UPDATE lab_files SET deleted_at = now() WHERE id = ?", lab);

        assertThat(
                        onlySeeded(
                                repository.findOverdue(PageRequest.of(0, 200)).getContent(),
                                Set.of(lab)))
                .isEmpty();
    }

    @Test
    void findOverdue_orders_lab_files_by_hours_past_deadline_desc() {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        var a = seedAnalysis("OR", "Order test", disciplineId, 0, 1);

        var lab1h = seedLabFile(patientId, Instant.now().minus(2, ChronoUnit.HOURS)); // 1h overdue
        var lab5h = seedLabFile(patientId, Instant.now().minus(6, ChronoUnit.HOURS)); // 5h overdue
        var lab3h = seedLabFile(patientId, Instant.now().minus(4, ChronoUnit.HOURS)); // 3h overdue
        seedAnalysisRequest(lab1h, a);
        seedAnalysisRequest(lab5h, a);
        seedAnalysisRequest(lab3h, a);

        var rows =
                onlySeeded(
                        repository.findOverdue(PageRequest.of(0, 200)).getContent(),
                        Set.of(lab1h, lab3h, lab5h));

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).getLabFileId()).isEqualTo(lab5h);
        assertThat(rows.get(1).getLabFileId()).isEqualTo(lab3h);
        assertThat(rows.get(2).getLabFileId()).isEqualTo(lab1h);
    }

    @Test
    void findOverdue_excludes_lab_file_with_zero_analysis_requests() {
        var patientId = seedPatient();
        var lab = seedLabFile(patientId, Instant.now().minus(72, ChronoUnit.HOURS));

        assertThat(
                        onlySeeded(
                                repository.findOverdue(PageRequest.of(0, 200)).getContent(),
                                Set.of(lab)))
                .isEmpty();
    }

    @Test
    void findOverdue_breaks_tied_deadlines_with_analysis_code_asc() {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        var aB = seedAnalysis("BBB", "B", disciplineId, 0, 1);
        var aA = seedAnalysis("AAA", "A", disciplineId, 0, 1); // same delay → same deadline
        var lab = seedLabFile(patientId, Instant.now().minus(3, ChronoUnit.HOURS));
        seedAnalysisRequest(lab, aB);
        seedAnalysisRequest(lab, aA);

        var rows =
                onlySeeded(
                        repository.findOverdue(PageRequest.of(0, 200)).getContent(), Set.of(lab));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getAnalysisCode()).startsWith("AAA");
    }

    @Test
    void findOverdue_ignores_soft_deleted_bio_validated_result() {
        // The `results.analysis_request_id` UNIQUE constraint blocks two rows per AR, so we test
        // the LEFT JOIN's `deleted_at IS NULL` filter with a single soft-deleted result. Without
        // the filter the bio-validated result would exclude the file; with the filter the LEFT
        // JOIN yields r.id IS NULL and the file remains overdue.
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        var a = seedAnalysis("RZ", "Result zombie", disciplineId, 0, 1);
        var lab = seedLabFile(patientId, Instant.now().minus(3, ChronoUnit.HOURS));
        var ar = seedAnalysisRequest(lab, a);
        var deletedId = seedResult(lab, ar, "BIOLOGICAL_VALIDATED");
        jdbc.update("UPDATE results SET deleted_at = now() WHERE id = ?", deletedId);

        assertThat(
                        onlySeeded(
                                repository.findOverdue(PageRequest.of(0, 200)).getContent(),
                                Set.of(lab)))
                .hasSize(1);
    }

    private static List<OverdueDossierRowProjection> onlySeeded(
            List<OverdueDossierRowProjection> rows, Set<String> seededIds) {
        return rows.stream().filter(r -> seededIds.contains(r.getLabFileId())).toList();
    }

    // --- Seed helpers ---

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
                "BIOCHEM-" + id.substring(10, 16));
        return id;
    }

    /** Nullable delay arguments support the null-delay test case in §8.2. */
    private String seedAnalysis(
            String code,
            String name,
            String disciplineId,
            @Nullable Integer delayDays,
            @Nullable Integer delayHours) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO analyses (id, code, name, kind, indiciary_value_b, label_count,"
                        + " discipline_id, result_type, delay_days, delay_hours, version) VALUES"
                        + " (?, ?, ?, 'SIMPLE', 10.00, 1, ?, 'REAL', ?, ?, 0)",
                id,
                code + "-" + id.substring(10, 16),
                name,
                disciplineId,
                delayDays,
                delayHours);
        return id;
    }

    private String seedLabFile(String patientId, Instant createdAt) {
        var id = Ulid.generate();
        var seq = String.format("%04d", REG_SEQ.getAndIncrement());
        var rn = "LF-OVR-" + seq;
        jdbc.update(
                "INSERT INTO lab_files (id, registration_number, patient_id, urgent, status,"
                        + " created_at, updated_at, version) VALUES (?, ?, ?, false, 'OPEN', ?, ?,"
                        + " 0)",
                id,
                rn,
                patientId,
                java.sql.Timestamp.from(createdAt),
                java.sql.Timestamp.from(createdAt));
        return id;
    }

    private String seedAnalysisRequest(String labFileId, String analysisId) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO analysis_requests (id, lab_file_id, analysis_id, analysis_code,"
                        + " requested_at, version) VALUES (?, ?, ?, (SELECT code FROM analyses"
                        + " WHERE id = ?), now(), 0)",
                id,
                labFileId,
                analysisId,
                analysisId);
        return id;
    }

    private String seedResult(String labFileId, String analysisRequestId, String state) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO results (id, lab_file_id, analysis_request_id, analysis_id,"
                    + " analysis_code_snapshot, patient_category_snapshot, patient_sex_snapshot,"
                    + " state, created_at, updated_at, version) VALUES (?, ?, ?, (SELECT"
                    + " analysis_id FROM analysis_requests WHERE id = ?), (SELECT analysis_code"
                    + " FROM analysis_requests WHERE id = ?), 'FEMME', 'F', ?, now(), now(), 0)",
                id,
                labFileId,
                analysisRequestId,
                analysisRequestId,
                analysisRequestId,
                state);
        return id;
    }
}
