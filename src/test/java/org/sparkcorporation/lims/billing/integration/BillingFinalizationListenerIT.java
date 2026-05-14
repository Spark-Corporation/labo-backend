package org.sparkcorporation.lims.billing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.billing.api.InvoiceApi;
import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.api.BiologicalValidationDone;
import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Integration test for the {@code BiologicalValidationDone → Invoice} event flow per spec §8.1.
 * Seeds a minimal patient + lab-file + analysis-request + bio-validated {@code result} stack via
 * raw JDBC (faster + tighter control than going through every REST endpoint), then publishes the
 * event and asserts the invoice is persisted with a properly-formatted number.
 *
 * <p>This is the first-failing test for chunk 5 — its existence drives the listener wiring and the
 * idempotency contract.
 */
@Tag("integration")
@SpringBootTest
class BillingFinalizationListenerIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ApplicationEventPublisher publisher;
    @Autowired private InvoiceApi invoiceApi;
    @Autowired private PlatformTransactionManager txManager;

    /**
     * Publish in a wrapping transaction — Spring Modulith's {@code @ApplicationModuleListener}
     * waits for AFTER_COMMIT to fire. Without a tx, the listener silently no-ops.
     */
    private void publishInTx(BiologicalValidationDone event) {
        new TransactionTemplate(txManager)
                .executeWithoutResult(status -> publisher.publishEvent(event));
    }

    @Test
    void bio_validation_issues_invoice_with_dated_sequence() {
        var fixture = seedReadyLabFile();

        publishInTx(
                new BiologicalValidationDone(
                        new ResultId(fixture.resultId()),
                        new LabFileId(fixture.labFileId()),
                        new AnalysisRequestId(fixture.analysisRequestId()),
                        "biologist-x",
                        Instant.now()));

        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(
                        () -> {
                            var maybe =
                                    invoiceApi.findByLabFileId(new LabFileId(fixture.labFileId()));
                            assertThat(maybe).isPresent();
                            var invoice = maybe.get();
                            assertThat(invoice.number().value())
                                    .matches("^[A-Z]{3}\\d{2}-\\d{4}-\\d{3,}$");
                            assertThat(invoice.labFileId().value()).isEqualTo(fixture.labFileId());
                            assertThat(invoice.lines()).hasSize(1);
                            assertThat(invoice.lines().get(0).analysisCodeSnapshot())
                                    .isEqualTo(fixture.analysisCode());
                        });
    }

    @Test
    void second_bio_validation_for_same_lab_file_is_noop() {
        var fixture = seedReadyLabFile();
        var event =
                new BiologicalValidationDone(
                        new ResultId(fixture.resultId()),
                        new LabFileId(fixture.labFileId()),
                        new AnalysisRequestId(fixture.analysisRequestId()),
                        "biologist-x",
                        Instant.now());

        publishInTx(event);
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(
                        () ->
                                assertThat(
                                                invoiceApi.findByLabFileId(
                                                        new LabFileId(fixture.labFileId())))
                                        .isPresent());

        var firstNumber =
                invoiceApi
                        .findByLabFileId(new LabFileId(fixture.labFileId()))
                        .orElseThrow()
                        .number()
                        .value();

        // Re-publish — idempotency guard must not create a second invoice.
        publishInTx(event);

        // Wait a bit and confirm no duplicate row.
        await().pollDelay(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            var maybe =
                                    invoiceApi.findByLabFileId(new LabFileId(fixture.labFileId()));
                            assertThat(maybe).isPresent();
                            assertThat(maybe.get().number().value()).isEqualTo(firstNumber);
                            Integer count =
                                    jdbc.queryForObject(
                                            "SELECT COUNT(*) FROM invoices WHERE lab_file_id = ?",
                                            Integer.class,
                                            fixture.labFileId());
                            assertThat(count).isEqualTo(1);
                        });
    }

    /**
     * Seed a fully-staged lab-file: patient, analysis, lab-file, analysis-request, biologically
     * validated result. Returns the relevant identifiers in a {@link Fixture}.
     */
    private Fixture seedReadyLabFile() {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        var analysisId = seedAnalysis(disciplineId);
        var analysisCode = lookupAnalysisCode(analysisId);
        var labFileId = seedLabFile(patientId);
        var analysisRequestId = seedAnalysisRequest(labFileId, analysisId, analysisCode);
        var techUserId = seedUser();
        var bioUserId = seedUser();
        var resultId =
                seedBioValidatedResult(
                        labFileId,
                        analysisRequestId,
                        analysisId,
                        analysisCode,
                        techUserId,
                        bioUserId);

        return new Fixture(
                patientId, labFileId, analysisRequestId, analysisId, resultId, analysisCode);
    }

    private String lookupAnalysisCode(String analysisId) {
        return jdbc.queryForObject(
                "SELECT code FROM analyses WHERE id = ?", String.class, analysisId);
    }

    private String seedUser() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO users (id, email, full_name, password_hash, enabled, version)"
                        + " VALUES (?, ?, 'Test User', '$2a$10$abcdefghijklmnopqrstuv', true, 0)",
                id,
                "u-" + id + "@test.local");
        return id;
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
                "INSERT INTO disciplines (id, code, name, discipline_prefix, version)"
                        + " VALUES (?, ?, 'Biochimie', 'BIOCHIMIE', 0)",
                id,
                "B" + id);
        return id;
    }

    private String seedAnalysis(String disciplineId) {
        var id = Ulid.generate();
        var code = "G" + id.substring(20, 26); // last 6 chars carry the random tail
        jdbc.update(
                "INSERT INTO analyses (id, code, name, kind, indiciary_value_b, label_count,"
                        + " discipline_id, result_type, version) VALUES (?, ?, 'Glucose',"
                        + " 'SIMPLE', 50.00, 1, ?, 'REAL', 0)",
                id,
                code,
                disciplineId);
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

    private String seedBioValidatedResult(
            String labFileId,
            String analysisRequestId,
            String analysisId,
            String analysisCode,
            String techUserId,
            String bioUserId) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO results (id, lab_file_id, analysis_request_id, analysis_id,"
                        + " analysis_code_snapshot, patient_category_snapshot,"
                        + " patient_sex_snapshot, state, technical_validated_by,"
                        + " technical_validated_at, biological_validated_by,"
                        + " biological_validated_at, version) VALUES (?, ?, ?, ?, ?, 'FEMME',"
                        + " 'F', 'BIOLOGICAL_VALIDATED', ?, now(), ?, now(), 0)",
                id,
                labFileId,
                analysisRequestId,
                analysisId,
                analysisCode,
                techUserId,
                bioUserId);
        return id;
    }

    private record Fixture(
            String patientId,
            String labFileId,
            String analysisRequestId,
            String analysisId,
            String resultId,
            String analysisCode) {}
}
