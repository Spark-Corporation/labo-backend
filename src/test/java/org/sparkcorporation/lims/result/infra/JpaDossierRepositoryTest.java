package org.sparkcorporation.lims.result.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

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
 * JPA-slice round-trip for {@link DossierRepository#findUrgent}. Seeds 3 urgent lab-files:
 *
 * <ol>
 *   <li>Urgent + DRAFT result → marker PEN, included by NON_RENDU_PREDICATE.
 *   <li>Urgent + out-of-range line + TECHNICAL_VALIDATED result → marker RED, included.
 *   <li>Urgent + all results BIOLOGICAL_VALIDATED → excluded by NON_RENDU_PREDICATE.
 * </ol>
 *
 * <p>Docker-blocked locally (Testcontainers PostgreSQL via {@link IntegrationTestBase}); runs in
 * CI.
 */
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaDossierRepositoryTest.AuditingTestConfig.class)
@DirtiesContext
class JpaDossierRepositoryTest extends IntegrationTestBase {

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
    void findUrgent_excludes_non_urgent_lab_files() {
        var nonUrgentLabFileId = seedLabFile(seedPatient(), false);
        var draftResultId =
                seedResult(nonUrgentLabFileId, seedAnalysisRequest(nonUrgentLabFileId), "DRAFT");
        assertThat(draftResultId).isNotNull();

        var page = repository.findUrgent(PageRequest.of(0, 100));
        var ids = page.getContent().stream().map(DossierRowProjection::getLabFileId).toList();
        assertThat(ids).doesNotContain(nonUrgentLabFileId);
    }

    @Test
    void findUrgent_includes_urgent_with_draft_result_as_PEN() {
        var patientId = seedPatient();
        var labFileId = seedLabFile(patientId, true);
        seedResult(labFileId, seedAnalysisRequest(labFileId), "DRAFT");

        var page = repository.findUrgent(PageRequest.of(0, 100));

        var row =
                page.getContent().stream()
                        .filter(p -> p.getLabFileId().equals(labFileId))
                        .findFirst();
        assertThat(row).isPresent();
        assertThat(row.get().getMarker()).isEqualTo("PEN");
        assertThat(row.get().getPatientId()).isEqualTo(patientId);
    }

    @Test
    void findUrgent_includes_urgent_with_out_of_range_line_and_technical_validated_as_RED() {
        var patientId = seedPatient();
        var labFileId = seedLabFile(patientId, true);
        var arId = seedAnalysisRequest(labFileId);
        var analysisId = extractAnalysisId(arId);
        var resultId = seedResult(labFileId, arId, "TECHNICAL_VALIDATED");
        seedOutOfRangeResultLine(resultId, analysisId);

        var page = repository.findUrgent(PageRequest.of(0, 100));

        var row =
                page.getContent().stream()
                        .filter(p -> p.getLabFileId().equals(labFileId))
                        .findFirst();
        assertThat(row).isPresent();
        assertThat(row.get().getMarker()).isEqualTo("RED");
    }

    @Test
    void findUrgent_excludes_fully_biological_validated_dossiers() {
        var patientId = seedPatient();
        var labFileId = seedLabFile(patientId, true);
        seedResult(labFileId, seedAnalysisRequest(labFileId), "BIOLOGICAL_VALIDATED");

        var page = repository.findUrgent(PageRequest.of(0, 100));

        var ids = page.getContent().stream().map(DossierRowProjection::getLabFileId).toList();
        assertThat(ids).doesNotContain(labFileId);
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

    private String seedLabFile(String patientId, boolean urgent) {
        var id = Ulid.generate();
        var rn = "LF-DOSS-" + id.substring(22, 26);
        jdbc.update(
                "INSERT INTO lab_files (id, registration_number, patient_id, urgent, status,"
                        + " created_at, updated_at, version) VALUES (?, ?, ?, ?, 'OPEN',"
                        + " now(), now(), 0)",
                id,
                rn,
                patientId,
                urgent);
        return id;
    }

    private String seedAnalysisRequest(String labFileId) {
        var disciplineId = Ulid.generate();
        jdbc.update(
                "INSERT INTO disciplines (id, code, name, discipline_prefix, version)"
                        + " VALUES (?, ?, 'Biochimie', 'BIOCHIMIE', 0)",
                disciplineId,
                "BIOCHEM-" + disciplineId.substring(10, 16));
        var analysisId = Ulid.generate();
        var code = "GLU-" + analysisId.substring(10, 16);
        jdbc.update(
                "INSERT INTO analyses (id, code, name, kind, indiciary_value_b, label_count,"
                        + " discipline_id, result_type, version)"
                        + " VALUES (?, ?, 'Glucose', 'SIMPLE', 10.00, 1, ?, 'REAL', 0)",
                analysisId,
                code,
                disciplineId);
        var arId = Ulid.generate();
        jdbc.update(
                "INSERT INTO analysis_requests (id, lab_file_id, analysis_id, analysis_code,"
                        + " requested_at, version) VALUES (?, ?, ?, ?, now(), 0)",
                arId,
                labFileId,
                analysisId,
                code);
        return arId;
    }

    private String extractAnalysisId(String analysisRequestId) {
        return jdbc.queryForObject(
                "SELECT analysis_id FROM analysis_requests WHERE id = ?",
                String.class,
                analysisRequestId);
    }

    private String seedResult(String labFileId, String analysisRequestId, String state) {
        var id = Ulid.generate();
        var analysisId = extractAnalysisId(analysisRequestId);
        var code =
                jdbc.queryForObject(
                        "SELECT analysis_code FROM analysis_requests WHERE id = ?",
                        String.class,
                        analysisRequestId);
        jdbc.update(
                "INSERT INTO results (id, lab_file_id, analysis_request_id, analysis_id,"
                        + " analysis_code_snapshot, patient_category_snapshot,"
                        + " patient_sex_snapshot, state, created_at, updated_at, version)"
                        + " VALUES (?, ?, ?, ?, ?, 'FEMME', 'F', ?, now(), now(), 0)",
                id,
                labFileId,
                analysisRequestId,
                analysisId,
                code,
                state);
        return id;
    }

    private void seedOutOfRangeResultLine(String resultId, String componentAnalysisId) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO result_lines (id, result_id, component_analysis_id,"
                        + " component_code_snapshot, position, result_type, is_out_of_range,"
                        + " created_at, updated_at, version) VALUES (?, ?, ?, 'GLU', 0, 'REAL',"
                        + " true, now(), now(), 0)",
                id,
                resultId,
                componentAnalysisId);
    }
}
