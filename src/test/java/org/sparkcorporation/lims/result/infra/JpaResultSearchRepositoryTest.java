package org.sparkcorporation.lims.result.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
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
 * JPA-slice round-trip for {@link ResultSearchRepository}. Docker-blocked locally (Testcontainers
 * PostgreSQL via {@link IntegrationTestBase}); runs in CI.
 *
 * <p>Seeds 2 patients (1 insured, 1 not), 2 lab-files, 2 results with different states and analysis
 * codes to exercise the §V filter predicates. Mirrors {@link JpaResultRepositoryTest} seed pattern.
 */
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaResultSearchRepositoryTest.AuditingTestConfig.class)
@DirtiesContext
class JpaResultSearchRepositoryTest extends IntegrationTestBase {

    @TestConfiguration
    @EnableJpaAuditing
    static class AuditingTestConfig {
        @Bean
        org.springframework.data.domain.AuditorAware<String> auditorAware() {
            return Optional::empty;
        }
    }

    @Autowired private ResultSearchRepository repository;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void search_with_no_filters_returns_all_active_lines() {
        seedFixture();
        var page =
                repository.search(
                        null, null, null, null, null, null, null, null, PageRequest.of(0, 100));
        assertThat(page.getTotalElements()).isEqualTo(2L);
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    void search_filters_by_analysis_code_against_result_snapshot() {
        seedFixture();
        var page =
                repository.search(
                        null, null, "GLU", null, null, null, null, null, PageRequest.of(0, 100));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getAnalysisCode()).isEqualTo("GLU");
    }

    @Test
    void search_filters_by_state_equals_validated() {
        seedFixture();
        var page =
                repository.search(
                        null,
                        null,
                        null,
                        null,
                        null,
                        "BIOLOGICAL_VALIDATED",
                        null,
                        null,
                        PageRequest.of(0, 100));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getResultState()).isEqualTo("BIOLOGICAL_VALIDATED");
    }

    @Test
    void search_filters_by_insured_patient() {
        seedFixture();
        var page =
                repository.search(
                        null,
                        null,
                        null,
                        Boolean.TRUE,
                        null,
                        null,
                        null,
                        null,
                        PageRequest.of(0, 100));
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void search_filters_by_partner_lab_patient_via_partner_kind() {
        seedFixtureWithPartnerLab();
        var page =
                repository.search(
                        null,
                        null,
                        null,
                        null,
                        "PARTNER_LAB",
                        null,
                        null,
                        null,
                        PageRequest.of(0, 100));
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void search_excludes_soft_deleted_results() {
        var ctx = seedMinimalResult("HEMO", false);
        jdbc.update("UPDATE results SET deleted_at = now() WHERE id = ?", ctx.resultId);
        var page =
                repository.search(
                        null, null, null, null, null, null, null, null, PageRequest.of(0, 100));
        assertThat(page.getContent()).noneMatch(r -> r.getResultId().equals(ctx.resultId));
    }

    private record SeedContext(String patientId, String labFileId, String resultId) {}

    private void seedFixture() {
        var insuranceId = seedInsurance();
        var insuredPatientId = seedPatient(insuranceId, null);
        var notInsuredPatientId = seedPatient(null, null);
        var disciplineId = seedDiscipline();
        var analysisGluId = seedAnalysis(disciplineId, "GLU");
        var analysisCbcId = seedAnalysis(disciplineId, "CBC");
        var labFile1 = seedLabFile(insuredPatientId);
        var labFile2 = seedLabFile(notInsuredPatientId);
        var arGlu = seedAnalysisRequest(labFile1, analysisGluId, "GLU");
        var arCbc = seedAnalysisRequest(labFile2, analysisCbcId, "CBC");
        var resultGlu = seedResult(labFile1, arGlu, analysisGluId, "GLU", "BIOLOGICAL_VALIDATED");
        var resultCbc = seedResult(labFile2, arCbc, analysisCbcId, "CBC", "DRAFT");
        seedResultLine(resultGlu, analysisGluId, "GLU");
        seedResultLine(resultCbc, analysisCbcId, "CBC");
    }

    private void seedFixtureWithPartnerLab() {
        var partnerId = seedPartner("PARTNER_LAB");
        var partnerPatientId = seedPatient(null, partnerId);
        var disciplineId = seedDiscipline();
        var analysisId = seedAnalysis(disciplineId, "GLU");
        var labFileId = seedLabFile(partnerPatientId);
        var arId = seedAnalysisRequest(labFileId, analysisId, "GLU");
        var resultId = seedResult(labFileId, arId, analysisId, "GLU", "DRAFT");
        seedResultLine(resultId, analysisId, "GLU");
    }

    private SeedContext seedMinimalResult(String analysisCode, boolean insured) {
        var patientId = insured ? seedPatient(seedInsurance(), null) : seedPatient(null, null);
        var disciplineId = seedDiscipline();
        var analysisId = seedAnalysis(disciplineId, analysisCode);
        var labFileId = seedLabFile(patientId);
        var arId = seedAnalysisRequest(labFileId, analysisId, analysisCode);
        var resultId = seedResult(labFileId, arId, analysisId, analysisCode, "DRAFT");
        seedResultLine(resultId, analysisId, analysisCode);
        return new SeedContext(patientId, labFileId, resultId);
    }

    private String seedPatient(
            @org.jspecify.annotations.Nullable String insuranceId,
            @org.jspecify.annotations.Nullable String partnerId) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO patients (id, lab_code, last_name, civility, sex, insured,"
                        + " insurance_id, partner_id, version)"
                        + " VALUES (?, ?, 'DOE', 'MADAME', 'F', ?, ?, ?, 0)",
                id,
                "LAB-" + id.substring(0, 26),
                insuranceId != null,
                insuranceId,
                partnerId);
        return id;
    }

    private String seedInsurance() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO insurances (id, code, name, b_rate, coverage_rate,"
                        + " created_at, updated_at) VALUES (?, ?, 'Acme Health', 200.00, 0.8000,"
                        + " now(), now())",
                id,
                id.substring(20, 26));
        return id;
    }

    private String seedPartner(String kind) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO partners (id, code, name, kind, b_rate,"
                        + " created_at, updated_at) VALUES (?, ?, 'Acme Partner', ?, 200.00,"
                        + " now(), now())",
                id,
                id.substring(20, 26),
                kind);
        return id;
    }

    private String seedDiscipline() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO disciplines (id, code, name, discipline_prefix, version)"
                        + " VALUES (?, ?, 'Biochimie', 'BIOCHIMIE', 0)",
                id,
                "BIO-" + id.substring(10, 16));
        return id;
    }

    private String seedAnalysis(String disciplineId, String code) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO analyses (id, code, name, kind, indiciary_value_b, label_count,"
                    + " discipline_id, result_type, version) VALUES (?, ?, ?, 'SIMPLE', 10.00, 1,"
                    + " ?, 'REAL', 0)",
                id,
                code + "-" + id.substring(10, 16),
                code,
                disciplineId);
        return id;
    }

    private String seedLabFile(String patientId) {
        var id = Ulid.generate();
        var rn = "LF-" + id.substring(22, 26);
        jdbc.update(
                "INSERT INTO lab_files (id, registration_number, patient_id, urgent, status,"
                        + " created_at, updated_at, version) VALUES (?, ?, ?, false, 'OPEN',"
                        + " now(), now(), 0)",
                id,
                rn,
                patientId);
        return id;
    }

    private String seedAnalysisRequest(String labFileId, String analysisId, String analysisCode) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO analysis_requests (id, lab_file_id, analysis_id, analysis_code,"
                        + " requested_at, version) VALUES (?, ?, ?, ?, now(), 0)",
                id,
                labFileId,
                analysisId,
                analysisCode);
        return id;
    }

    private String seedResult(
            String labFileId, String arId, String analysisId, String analysisCode, String state) {
        var id = Ulid.generate();
        var biovalAt = "BIOLOGICAL_VALIDATED".equals(state) ? Instant.now().toString() : null;
        jdbc.update(
                "INSERT INTO results (id, lab_file_id, analysis_request_id, analysis_id,"
                        + " analysis_code_snapshot, patient_category_snapshot,"
                        + " patient_sex_snapshot, state, biological_validated_at,"
                        + " created_at, updated_at, version) VALUES (?, ?, ?, ?, ?, 'FEMME', 'F',"
                        + " ?, ?::timestamp, now(), now(), 0)",
                id,
                labFileId,
                arId,
                analysisId,
                analysisCode,
                state,
                biovalAt);
        return id;
    }

    private void seedResultLine(String resultId, String componentAnalysisId, String componentCode) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO result_lines (id, result_id, component_analysis_id,"
                        + " component_code_snapshot, position, result_type, is_out_of_range,"
                        + " created_at, updated_at, version) VALUES (?, ?, ?, ?, 1, 'REAL', false,"
                        + " now(), now(), 0)",
                id,
                resultId,
                componentAnalysisId,
                componentCode);
    }
}
