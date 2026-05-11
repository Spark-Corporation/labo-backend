package org.sparkcorporation.lims.result.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.annotation.DirtiesContext;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.domain.NewResult;
import org.sparkcorporation.lims.result.domain.Result;
import org.sparkcorporation.lims.shared.domain.PatientCategory;
import org.sparkcorporation.lims.shared.domain.Sex;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * JPA-slice round-trip for {@link ResultRepository}. Docker-blocked locally (Testcontainers
 * PostgreSQL via {@link IntegrationTestBase}); runs in CI.
 *
 * <p>Seeds cross-module FKs (patients, disciplines, analyses, lab_files, analysis_requests) via raw
 * SQL — the result module cannot import other modules' entities (Modulith boundary; mirrors {@code
 * JpaLabFileRepositoryTest} chunk-4 precedent).
 */
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaResultRepositoryTest.AuditingTestConfig.class)
@DirtiesContext
class JpaResultRepositoryTest extends IntegrationTestBase {

    /**
     * {@code @DataJpaTest} does not bootstrap the production {@code IamSecurityConfig} (which owns
     * the project's {@code @EnableJpaAuditing}). Without auditing the {@code @CreatedDate} /
     * {@code @LastModifiedDate} fields on {@link
     * org.sparkcorporation.lims.result.domain.ResultLine} stay {@code null} and Hibernate sends an
     * explicit {@code NULL} on insert that bypasses the Liquibase {@code defaultValueComputed:
     * now()} default — violating the column's NOT NULL constraint. Importing this
     * {@code @TestConfiguration} re-enables auditing for the slice.
     */
    @TestConfiguration
    @EnableJpaAuditing
    static class AuditingTestConfig {
        @Bean
        AuditorAware<String> auditorAware() {
            return Optional::empty;
        }
    }

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-09T08:00:00Z"), ZoneOffset.UTC);

    @Autowired private ResultRepository repo;
    @Autowired private EntityManager em;

    @Test
    void findActiveById_ignores_soft_deleted_rows() {
        var ctx = seedCrossModuleParents();
        var result =
                Result.create(
                        new NewResult(
                                new LabFileId(ctx.labFileId),
                                new AnalysisRequestId(ctx.analysisRequestId),
                                new AnalysisId(ctx.analysisId),
                                ctx.analysisCode,
                                AnalysisKind.SIMPLE,
                                ResultType.REAL,
                                List.of(),
                                Sex.F,
                                30 * 365,
                                PatientCategory.FEMME,
                                "mg/dL",
                                2,
                                null,
                                null),
                        FIXED);
        repo.saveAndFlush(result);

        // Soft-delete via @SQLDelete trigger.
        repo.deleteById(result.getId());
        repo.flush();
        em.clear();

        // Raw row still exists (soft delete) but findActiveById filters it.
        assertThat(repo.findById(result.getId())).isPresent();
        assertThat(repo.findById(result.getId()).orElseThrow().getDeletedAt()).isNotNull();
        assertThat(repo.findActiveById(result.getId())).isEmpty();
    }

    @Test
    void findActiveByLabFileId_returns_results_ordered_by_analysis_code_snapshot() {
        // Seed three results on the same lab file with codes that sort A < G < Z.
        var ctxZ = seedCrossModuleParents("ZINC");
        var ctxA = seedCrossModuleParentsForLabFile(ctxZ.labFileId, ctxZ.patientId, "ALBU");
        var ctxG = seedCrossModuleParentsForLabFile(ctxZ.labFileId, ctxZ.patientId, "GLUC");

        // Insert in non-alpha order to confirm ORDER BY actually fires.
        repo.saveAndFlush(buildSimpleResult(ctxZ));
        repo.saveAndFlush(buildSimpleResult(ctxA));
        repo.saveAndFlush(buildSimpleResult(ctxG));
        em.clear();

        List<Result> active = repo.findActiveByLabFileId(ctxZ.labFileId);

        assertThat(active)
                .extracting(Result::getAnalysisCodeSnapshot)
                .containsExactly("ALBU", "GLUC", "ZINC");
    }

    private Result buildSimpleResult(SeedContext ctx) {
        return Result.create(
                new NewResult(
                        new LabFileId(ctx.labFileId),
                        new AnalysisRequestId(ctx.analysisRequestId),
                        new AnalysisId(ctx.analysisId),
                        ctx.analysisCode,
                        AnalysisKind.SIMPLE,
                        ResultType.REAL,
                        List.of(),
                        Sex.F,
                        30 * 365,
                        PatientCategory.FEMME,
                        "mg/dL",
                        2,
                        null,
                        null),
                FIXED);
    }

    private record SeedContext(
            String patientId,
            String disciplineId,
            String analysisId,
            String analysisCode,
            String labFileId,
            String analysisRequestId) {}

    private SeedContext seedCrossModuleParents() {
        return seedCrossModuleParents("GLUC");
    }

    private SeedContext seedCrossModuleParents(String analysisCode) {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        var analysisId = seedAnalysis(disciplineId, analysisCode);
        var labFileId = seedLabFile(patientId);
        var analysisRequestId = seedAnalysisRequest(labFileId, analysisId, analysisCode);
        return new SeedContext(
                patientId, disciplineId, analysisId, analysisCode, labFileId, analysisRequestId);
    }

    private SeedContext seedCrossModuleParentsForLabFile(
            String labFileId, String patientId, String analysisCode) {
        var disciplineId = seedDiscipline();
        var analysisId = seedAnalysis(disciplineId, analysisCode);
        var analysisRequestId = seedAnalysisRequest(labFileId, analysisId, analysisCode);
        return new SeedContext(
                patientId, disciplineId, analysisId, analysisCode, labFileId, analysisRequestId);
    }

    private String seedPatient() {
        var id = Ulid.generate();
        em.createNativeQuery(
                        "INSERT INTO patients (id, lab_code, last_name, civility, sex, insured,"
                                + " version) VALUES (?, ?, 'DOE', 'MADAME', 'F', false, 0)")
                .setParameter(1, id)
                .setParameter(2, "LAB-" + id.substring(0, 10))
                .executeUpdate();
        return id;
    }

    private String seedDiscipline() {
        var id = Ulid.generate();
        em.createNativeQuery(
                        "INSERT INTO disciplines (id, code, name, discipline_prefix, version)"
                                + " VALUES (?, ?, 'Biochimie', 'BIOCHIMIE', 0)")
                .setParameter(1, id)
                .setParameter(2, "BIOCHEM-" + id.substring(10, 16))
                .executeUpdate();
        return id;
    }

    private String seedAnalysis(String disciplineId, String code) {
        var id = Ulid.generate();
        em.createNativeQuery(
                        "INSERT INTO analyses (id, code, name, kind, indiciary_value_b,"
                                + " label_count, discipline_id, result_type, version) VALUES (?, ?,"
                                + " ?, 'SIMPLE', 10.00, 1, ?, 'REAL', 0)")
                .setParameter(1, id)
                .setParameter(2, code + "-" + id.substring(10, 16))
                .setParameter(3, code)
                .setParameter(4, disciplineId)
                .executeUpdate();
        return id;
    }

    private String seedLabFile(String patientId) {
        var id = Ulid.generate();
        // registration_number is unique; suffix with 4 digits derived from the ULID.
        var rn = "LF-260509-" + id.substring(22, 26);
        em.createNativeQuery(
                        "INSERT INTO lab_files (id, registration_number, patient_id, urgent,"
                                + " status, created_at, updated_at, version) VALUES (?, ?, ?,"
                                + " false, 'OPEN', now(), now(), 0)")
                .setParameter(1, id)
                .setParameter(2, rn)
                .setParameter(3, patientId)
                .executeUpdate();
        return id;
    }

    private String seedAnalysisRequest(String labFileId, String analysisId, String analysisCode) {
        var id = Ulid.generate();
        em.createNativeQuery(
                        "INSERT INTO analysis_requests (id, lab_file_id, analysis_id,"
                            + " analysis_code, requested_at, version) VALUES (?, ?, ?, ?, now(),"
                            + " 0)")
                .setParameter(1, id)
                .setParameter(2, labFileId)
                .setParameter(3, analysisId)
                .setParameter(4, analysisCode)
                .executeUpdate();
        return id;
    }
}
