package org.sparkcorporation.lims.stats;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Shared support for the {@code /api/v1/stats/*} acceptance ITs (spec §9). Seeds the operational
 * graph the native-SQL read model aggregates — patients, lab-files, the catalog rows
 * (disciplines/analyses/prescribers/antibiotics/reference-intervals) and the result graph
 * (analysis-requests/results/result-lines/antibiogram-readings) — via raw JDBC for tight control
 * over the temporal and value predicates the queries exercise.
 *
 * <p>The {@code seedPatient}/{@code seedLabFile} helpers are adapted from {@code
 * billing/integration/AccountingControllerIT}: they satisfy the NOT-NULL columns of {@code
 * patients}/{@code lab_files} and additionally let the caller drive {@code patients.created_at}
 * (load-bearing for the new/returning cohort split, §6.3) and the optional lab-file filter FKs.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class StatsIntegrationTestBase extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    @Autowired protected JdbcTemplate jdbc;

    /**
     * The five {@code DisciplinePrefix} codes seeded by the Liquibase reference data ({@code
     * 202605061501-…}). Per-test disciplines use disjoint codes (e.g. {@code BIO}, {@code HEMA})
     * and are cleaned each test; these seed rows are preserved.
     */
    private static final String SEEDED_DISCIPLINE_CODES =
            "('BIOCHIMIE','HEMATOLOGIE','IMMUNO_SEROLOGIE','MICROBIOLOGIE','BIOLOGIE_MOLECULAIRE')";

    @BeforeEach
    void cleanStatsGraph() {
        // FK order: leaves first. Catalog rows seeded per-test (analyses/prescribers/disciplines/
        // antibiotics/reference-intervals) are removed too — they carry test-chosen unique keys
        // (e.g. disciplines.code) that would otherwise collide across tests. analyses and
        // prescribers carry no Liquibase seed rows, so they are emptied wholesale; disciplines and
        // antibiotics preserve their reference-seed rows by predicate.
        //
        // lab_files is wiped wholesale, so EVERY table that FK-references lab_files must be cleared
        // first, or a sibling IT's leftover rows in the shared container DB block the delete. The
        // full child set is: antibiogram_readings/result_lines → results → analysis_requests;
        // payments/invoice_lines → invoices; quote_lines → quotes; plus samples and
        // dossier_comments (payments also references lab_files directly).
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
        jdbc.update(
                "DELETE FROM reference_intervals WHERE category = 'HOMME' AND mode = 'STANDARD'");
        jdbc.update("DELETE FROM antibiotics WHERE reference_diameter = 10.0");
        jdbc.update("DELETE FROM analyses");
        jdbc.update("DELETE FROM disciplines WHERE code NOT IN " + SEEDED_DISCIPLINE_CODES);
        jdbc.update("DELETE FROM prescribers");
    }

    protected static Timestamp ts(Instant i) {
        return Timestamp.from(i);
    }

    /**
     * Inserts a patient with the minimal NOT-NULL set, overriding {@code sex}, {@code birth_date}
     * and {@code created_at} (the cohort split keys on the latter).
     */
    protected String seedPatient(String sex, @Nullable LocalDate birthDate, Instant createdAt) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO patients (id, lab_code, last_name, civility, sex, birth_date, insured,"
                        + " created_at, version) VALUES (?, ?, 'DOE', 'MADAME', ?, ?, false, ?, 0)",
                id,
                "LAB-" + id.substring(0, 10),
                sex,
                birthDate == null ? null : java.sql.Date.valueOf(birthDate),
                ts(createdAt));
        return id;
    }

    /**
     * Inserts a lab-file owned by {@code patientId}, registered at {@code createdAt}, with the
     * given urgency/status and optional filter FKs ({@code insuranceId}/{@code partnerId}/{@code
     * prescriberId}). {@code deleted_at} is left null (surviving dossier).
     */
    protected String seedLabFile(
            String patientId,
            Instant createdAt,
            boolean urgent,
            String status,
            @Nullable String insuranceId,
            @Nullable String partnerId,
            @Nullable String prescriberId) {
        var id = Ulid.generate();
        var seq = String.format("%04d", REG_SEQ.getAndIncrement());
        var regNum = "LF-" + LocalDate.now().format(DATE_FMT) + "-" + seq;
        jdbc.update(
                "INSERT INTO lab_files (id, registration_number, patient_id, prescriber_id, urgent,"
                        + " status, created_at, updated_at, deleted_at, insurance_id, partner_id,"
                        + " version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, 0)",
                id,
                regNum,
                patientId,
                prescriberId,
                urgent,
                status,
                ts(createdAt),
                ts(createdAt),
                insuranceId,
                partnerId);
        return id;
    }

    protected String seedDiscipline(String code, String name) {
        String id = Ulid.generate();
        // discipline_prefix is NOT NULL (202605071400-add-discipline-prefix.yaml); reuse the code
        // as the prefix for test rows (the stats queries never read this column).
        jdbc.update(
                "INSERT INTO disciplines(id,code,name,discipline_prefix,created_at,updated_at,"
                        + "version) VALUES (?,?,?,?,now(),now(),0)",
                id,
                code,
                name,
                code);
        return id;
    }

    protected String seedAnalysis(String code, String name, String disciplineId) {
        String id = Ulid.generate();
        jdbc.update(
                "INSERT INTO analyses(id,code,name,kind,indiciary_value_b,discipline_id,"
                        + "result_type,created_at,updated_at,version) "
                        + "VALUES (?,?,?,'SIMPLE',1,?, 'REAL', now(), now(), 0)",
                id,
                code,
                name,
                disciplineId);
        return id;
    }

    protected String seedPrescriber(String code, String name) {
        String id = Ulid.generate();
        jdbc.update(
                "INSERT INTO prescribers(id,code,name,created_at,updated_at,version) "
                        + "VALUES (?,?,?,now(),now(),0)",
                id,
                code,
                name);
        return id;
    }

    /** mode 'STANDARD' with numeric bounds for élevé/bas tests. */
    protected String seedReferenceInterval(String analysisId, double min, double max) {
        String id = Ulid.generate();
        jdbc.update(
                "INSERT INTO"
                    + " reference_intervals(id,analysis_id,category,mode,unit_1_min,unit_1_max,created_at,updated_at,version)"
                    + " VALUES (?,?, 'HOMME', 'STANDARD', ?, ?, now(), now(), 0)",
                id,
                analysisId,
                min,
                max);
        return id;
    }

    /** state e.g. 'BIOLOGICAL_VALIDATED'; category e.g. 'HOMME'; sex 'M'/'F'. */
    protected String seedResult(
            String labFileId,
            String analysisId,
            String analysisCode,
            String state,
            String category,
            String sex,
            @Nullable Integer ageDays) {
        String id = Ulid.generate();
        String reqId = Ulid.generate();
        jdbc.update(
                "INSERT INTO analysis_requests(id,lab_file_id,analysis_id,analysis_code,"
                        + "requested_at,version) VALUES (?,?,?,?,now(),0)",
                reqId,
                labFileId,
                analysisId,
                analysisCode);
        jdbc.update(
                "INSERT INTO results(id,lab_file_id,analysis_request_id,analysis_id,"
                        + "analysis_code_snapshot,patient_category_snapshot,patient_sex_snapshot,"
                        + "patient_age_in_days_snapshot,state,created_at,updated_at,version) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,now(),now(),0)",
                id,
                labFileId,
                reqId,
                analysisId,
                analysisCode,
                category,
                sex,
                ageDays,
                state);
        return id;
    }

    protected void seedResultLine(
            String resultId,
            String componentAnalysisId,
            String componentCode,
            @Nullable Double valueReal,
            @Nullable String refIntervalId,
            boolean outOfRange,
            int position) {
        jdbc.update(
                "INSERT INTO result_lines(id,result_id,component_analysis_id,"
                        + "component_code_snapshot,position,result_type,value_real,"
                        + "reference_interval_id,is_out_of_range,created_at,updated_at,version) "
                        + "VALUES (?,?,?,?,?, 'REAL', ?, ?, ?, now(), now(), 0)",
                Ulid.generate(),
                resultId,
                componentAnalysisId,
                componentCode,
                position,
                valueReal,
                refIntervalId,
                outOfRange);
    }

    protected String seedAntibiotic(String code, String name) {
        String id = Ulid.generate();
        // antibiotics NOT-NULL set (202605061500-create-catalog-reference-tables.yaml):
        // id, code, name, reference_diameter (numeric, no default). created_at/updated_at default
        // now(); version 0.
        jdbc.update(
                "INSERT INTO antibiotics(id,code,name,reference_diameter,created_at,updated_at,"
                        + "version) VALUES (?,?,?, 10.0, now(), now(), 0)",
                id,
                code,
                name);
        return id;
    }

    protected void seedAntibiogramReading(
            String resultId, String antibioticId, char sir, int position) {
        jdbc.update(
                "INSERT INTO antibiogram_readings(id,result_id,antibiotic_id,measured_diameter,"
                        + "interpreted_sensitivity,position,created_at,updated_at,version) "
                        + "VALUES (?,?,?, 20.0, ?, ?, now(), now(), 0)",
                Ulid.generate(),
                resultId,
                antibioticId,
                String.valueOf(sir),
                position);
    }
}
