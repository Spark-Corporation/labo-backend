package org.sparkcorporation.lims.stats.infra;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Read-side projections for the CDC "Onglet Statistiques" endpoints (spec §6, plan chunk 1+). Each
 * public method holds one fixed native-SQL fragment, conditionally appends bound-parameter
 * predicates for the optional filters, and maps the aggregate with an inline {@code RowMapper} —
 * structurally identical to {@code billing/infra/AccountingReadModel}.
 *
 * <p>The date range is half-open {@code [from, toExclusive)} where {@code toExclusive = to + 1 day}
 * (spec §6.2), bound as {@link Timestamp} of the day start. Every query that touches a result row
 * additionally scopes on {@code lf.deleted_at IS NULL} (and, for result-grain queries, {@code
 * r.deleted_at IS NULL AND r.state <> 'DRAFT'}). Optional filters never interpolate identifiers —
 * only the presence of a fixed predicate is toggled.
 */
@Component
@RequiredArgsConstructor
public class StatsReadModel {

    private static final String BY_DISCIPLINE_SQL =
            """
            SELECT d.id AS discipline_id, d.code AS discipline_code, d.name AS discipline_name,
                   COUNT(rl.id) AS result_count,
                   COUNT(rl.id) FILTER (WHERE rl.is_out_of_range) AS abnormal_count
            FROM result_lines rl
            JOIN results r     ON r.id = rl.result_id AND r.deleted_at IS NULL AND r.state <> 'DRAFT'
            JOIN lab_files lf  ON lf.id = r.lab_file_id AND lf.deleted_at IS NULL
            JOIN analyses a    ON a.id = rl.component_analysis_id
            JOIN disciplines d ON d.id = a.discipline_id
            WHERE lf.created_at >= ? AND lf.created_at < ?
            """;

    private static final String BY_PRESCRIBER_SQL =
            """
            SELECT pr.id AS prescriber_id, pr.code AS prescriber_code, pr.name AS prescriber_name,
                   COUNT(*) AS dossier_count
            FROM lab_files lf
            JOIN prescribers pr ON pr.id = lf.prescriber_id
            WHERE lf.deleted_at IS NULL AND lf.created_at >= ? AND lf.created_at < ?
            """;

    private static final String ANALYSES_SQL =
            """
            SELECT a.id AS analysis_id, a.code AS analysis_code, a.name AS analysis_name,
                   COUNT(rl.id)                                          AS resulted_count,
                   COUNT(rl.id) FILTER (WHERE NOT rl.is_out_of_range)    AS normal_count,
                   COUNT(rl.id) FILTER (WHERE rl.is_out_of_range AND ri.mode = 'STANDARD'
                         AND COALESCE(rl.value_real, rl.value_integer) > ri.unit_1_max) AS high_count,
                   COUNT(rl.id) FILTER (WHERE rl.is_out_of_range AND ri.mode = 'STANDARD'
                         AND COALESCE(rl.value_real, rl.value_integer) < ri.unit_1_min) AS low_count
            FROM result_lines rl
            JOIN results r    ON r.id = rl.result_id AND r.deleted_at IS NULL AND r.state <> 'DRAFT'
            JOIN lab_files lf ON lf.id = r.lab_file_id AND lf.deleted_at IS NULL
            JOIN analyses a   ON a.id = rl.component_analysis_id
            LEFT JOIN reference_intervals ri ON ri.id = rl.reference_interval_id
            WHERE lf.created_at >= ? AND lf.created_at < ?
            """;

    private static final String ANTIBIOGRAMS_SQL =
            """
            SELECT ab.id AS antibiotic_id, ab.code AS antibiotic_code, ab.name AS antibiotic_name,
                   COUNT(*) FILTER (WHERE ar.interpreted_sensitivity = 'S') AS sensible,
                   COUNT(*) FILTER (WHERE ar.interpreted_sensitivity = 'I') AS intermediate,
                   COUNT(*) FILTER (WHERE ar.interpreted_sensitivity = 'R') AS resistant
            FROM antibiogram_readings ar
            JOIN results r      ON r.id = ar.result_id AND r.deleted_at IS NULL AND r.state <> 'DRAFT'
            JOIN lab_files lf   ON lf.id = r.lab_file_id AND lf.deleted_at IS NULL
            JOIN antibiotics ab ON ab.id = ar.antibiotic_id
            WHERE lf.created_at >= ? AND lf.created_at < ?
            """;

    private static final String PATIENTS_SQL =
            """
            SELECT
              COUNT(DISTINCT lf.patient_id) AS all_count,
              COUNT(DISTINCT lf.patient_id)
                FILTER (WHERE p.created_at >= ? AND p.created_at < ?) AS new_count,
              COUNT(DISTINCT lf.patient_id) FILTER (WHERE p.sex = 'M') AS male,
              COUNT(DISTINCT lf.patient_id) FILTER (WHERE p.sex = 'F') AS female
            FROM lab_files lf
            JOIN patients p ON p.id = lf.patient_id
            WHERE lf.deleted_at IS NULL AND lf.created_at >= ? AND lf.created_at < ?
            """;

    private static final String PATIENTS_BY_CATEGORY_SQL =
            """
            SELECT r.patient_category_snapshot AS category, COUNT(DISTINCT lf.patient_id) AS cnt
            FROM results r
            JOIN lab_files lf ON lf.id = r.lab_file_id AND lf.deleted_at IS NULL
            WHERE r.deleted_at IS NULL AND r.state <> 'DRAFT'
              AND lf.created_at >= ? AND lf.created_at < ?
            """;

    private final JdbcTemplate jdbc;

    private static Timestamp startOf(LocalDate d) {
        return Timestamp.valueOf(d.atStartOfDay());
    }

    private static void requireOrdered(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "from must be <= to (got from=" + from + ", to=" + to + ")");
        }
    }

    /**
     * Activity recap over dossiers registered in {@code [from, to]} (spec §5.1): total dossiers,
     * urgent and biologically-validated counts, and the distinct-patient cohort split (all vs new,
     * the latter being patients whose {@code created_at} falls inside the range).
     */
    public ActivityRow activity(
            LocalDate from,
            LocalDate to,
            @Nullable String insuranceId,
            @Nullable String partnerId,
            @Nullable String prescriberId,
            @Nullable String sex) {
        requireOrdered(from, to);
        Timestamp f = startOf(from);
        Timestamp tx = startOf(to.plusDays(1));
        var sql =
                new StringBuilder(
                        """
                        SELECT
                          COUNT(*) AS dossiers,
                          COUNT(*) FILTER (WHERE lf.urgent) AS urgents,
                          COUNT(*) FILTER (WHERE lf.status IN ('BIOLOGICALLY_VALIDATED','PUBLISHED')) AS validated,
                          COUNT(DISTINCT lf.patient_id) AS patients_all,
                          COUNT(DISTINCT lf.patient_id)
                            FILTER (WHERE p.created_at >= ? AND p.created_at < ?) AS patients_new
                        FROM lab_files lf
                        JOIN patients p ON p.id = lf.patient_id
                        WHERE lf.deleted_at IS NULL AND lf.created_at >= ? AND lf.created_at < ?
                        """);
        var args = new ArrayList<Object>(List.of(f, tx, f, tx)); // FILTER range, then WHERE range
        if (insuranceId != null) {
            sql.append(" AND lf.insurance_id = ?");
            args.add(insuranceId);
        }
        if (partnerId != null) {
            sql.append(" AND lf.partner_id = ?");
            args.add(partnerId);
        }
        if (prescriberId != null) {
            sql.append(" AND lf.prescriber_id = ?");
            args.add(prescriberId);
        }
        if (sex != null) {
            sql.append(" AND p.sex = ?");
            args.add(sex);
        }
        return jdbc.queryForObject(
                sql.toString(),
                (rs, n) ->
                        new ActivityRow(
                                rs.getLong("dossiers"),
                                rs.getLong("urgents"),
                                rs.getLong("validated"),
                                rs.getLong("patients_all"),
                                rs.getLong("patients_new")),
                args.toArray());
    }

    /**
     * Per-discipline resulted-line counts over dossiers registered in {@code [from, to]} (spec
     * §5.2). The discipline is that of each line's <em>component</em> analysis (spec §5 "Discipline
     * attribution"); {@code abnormalCount} is the out-of-range subset of {@code resultCount}.
     */
    public List<DisciplineStatRow> byDiscipline(
            LocalDate from,
            LocalDate to,
            @Nullable String insuranceId,
            @Nullable String partnerId) {
        requireOrdered(from, to);
        Timestamp f = startOf(from);
        Timestamp tx = startOf(to.plusDays(1));
        var sql = new StringBuilder(BY_DISCIPLINE_SQL);
        var args = new ArrayList<Object>(List.of(f, tx));
        if (insuranceId != null) {
            sql.append(" AND lf.insurance_id = ?");
            args.add(insuranceId);
        }
        if (partnerId != null) {
            sql.append(" AND lf.partner_id = ?");
            args.add(partnerId);
        }
        sql.append(" GROUP BY d.id, d.code, d.name ORDER BY d.name");
        return jdbc.query(
                sql.toString(),
                (rs, n) ->
                        new DisciplineStatRow(
                                rs.getString("discipline_id"),
                                rs.getString("discipline_code"),
                                rs.getString("discipline_name"),
                                rs.getLong("result_count"),
                                rs.getLong("abnormal_count")),
                args.toArray());
    }

    /**
     * Per-prescriber dossier counts over dossiers registered in {@code [from, to]} (spec §5.3). The
     * inner join on {@code prescribers} deliberately excludes dossiers with no prescriber ({@code
     * lab_files.prescriber_id IS NULL}) — they cannot be attributed to one (plan task 2.2). The
     * optional {@code disciplineId} narrows to dossiers having an analysis request in that
     * discipline.
     */
    public List<PrescriberStatRow> byPrescriber(
            LocalDate from, LocalDate to, @Nullable String disciplineId) {
        requireOrdered(from, to);
        Timestamp f = startOf(from);
        Timestamp tx = startOf(to.plusDays(1));
        var sql = new StringBuilder(BY_PRESCRIBER_SQL);
        var args = new ArrayList<Object>(List.of(f, tx));
        if (disciplineId != null) {
            sql.append(
                    " AND EXISTS (SELECT 1 FROM analysis_requests ar"
                            + " JOIN analyses a ON a.id = ar.analysis_id"
                            + " WHERE ar.lab_file_id = lf.id AND a.discipline_id = ?)");
            args.add(disciplineId);
        }
        sql.append(" GROUP BY pr.id, pr.code, pr.name ORDER BY dossier_count DESC, pr.name");
        return jdbc.query(
                sql.toString(),
                (rs, n) ->
                        new PrescriberStatRow(
                                rs.getString("prescriber_id"),
                                rs.getString("prescriber_code"),
                                rs.getString("prescriber_name"),
                                rs.getLong("dossier_count")),
                args.toArray());
    }

    /**
     * Per-analysis resulted-line breakdown over dossiers registered in {@code [from, to]} (spec
     * §5.4). Grouped by each line's <em>component</em> analysis: {@code resultedCount} is the
     * total, {@code normalCount} the in-range subset, and {@code highCount}/{@code lowCount} the
     * élevé/bas split derived only from out-of-range lines backed by a numeric {@code
     * STANDARD}-mode reference interval (qualitative abnormals contribute to neither — spec §6.4).
     * The optional {@code disciplineId} narrows to analyses in that discipline.
     */
    public List<AnalysisStatRow> analyses(
            LocalDate from, LocalDate to, @Nullable String disciplineId) {
        requireOrdered(from, to);
        Timestamp f = startOf(from);
        Timestamp tx = startOf(to.plusDays(1));
        var sql = new StringBuilder(ANALYSES_SQL);
        var args = new ArrayList<Object>(List.of(f, tx));
        if (disciplineId != null) {
            sql.append(" AND a.discipline_id = ?");
            args.add(disciplineId);
        }
        sql.append(" GROUP BY a.id, a.code, a.name ORDER BY a.code");
        return jdbc.query(
                sql.toString(),
                (rs, n) ->
                        new AnalysisStatRow(
                                rs.getString("analysis_id"),
                                rs.getString("analysis_code"),
                                rs.getString("analysis_name"),
                                rs.getLong("resulted_count"),
                                rs.getLong("normal_count"),
                                rs.getLong("high_count"),
                                rs.getLong("low_count")),
                args.toArray());
    }

    /**
     * Per-antibiotic S/I/R counts over antibiogram readings whose owning result was registered in
     * {@code [from, to]} (spec §5.5, §6.5). The optional {@code disciplineId} narrows to readings
     * whose result's analysis belongs to that discipline.
     */
    public List<AntibiogramStatRow> antibiograms(
            LocalDate from, LocalDate to, @Nullable String disciplineId) {
        requireOrdered(from, to);
        Timestamp f = startOf(from);
        Timestamp tx = startOf(to.plusDays(1));
        var sql = new StringBuilder(ANTIBIOGRAMS_SQL);
        var args = new ArrayList<Object>(List.of(f, tx));
        if (disciplineId != null) {
            sql.append(
                    " AND EXISTS (SELECT 1 FROM analyses a"
                            + " WHERE a.id = r.analysis_id AND a.discipline_id = ?)");
            args.add(disciplineId);
        }
        sql.append(" GROUP BY ab.id, ab.code, ab.name ORDER BY ab.name");
        return jdbc.query(
                sql.toString(),
                (rs, n) ->
                        new AntibiogramStatRow(
                                rs.getString("antibiotic_id"),
                                rs.getString("antibiotic_code"),
                                rs.getString("antibiotic_name"),
                                rs.getLong("sensible"),
                                rs.getLong("intermediate"),
                                rs.getLong("resistant")),
                args.toArray());
    }

    /**
     * Patient demographics over dossiers registered in {@code [from, to]} (spec §5.6, §6.6),
     * computed at two grains: the patient grain ({@code allCount}, the new/returning cohort split,
     * and the {@code M}/{@code F} sex split) via {@code lab_files} ⋈ {@code patients}; and the
     * {@code byAgeBand} distinct-patient counts per {@code results.patient_category_snapshot}. Both
     * grains accept the optional {@code insuranceId}/{@code partnerId} lab-file filters.
     */
    public PatientStatsRow patients(
            LocalDate from,
            LocalDate to,
            @Nullable String insuranceId,
            @Nullable String partnerId) {
        requireOrdered(from, to);
        Timestamp f = startOf(from);
        Timestamp tx = startOf(to.plusDays(1));

        var sql = new StringBuilder(PATIENTS_SQL);
        var args = new ArrayList<Object>(List.of(f, tx, f, tx)); // FILTER range, then WHERE range
        if (insuranceId != null) {
            sql.append(" AND lf.insurance_id = ?");
            args.add(insuranceId);
        }
        if (partnerId != null) {
            sql.append(" AND lf.partner_id = ?");
            args.add(partnerId);
        }
        record PatientGrain(long allCount, long newCount, long male, long female) {}
        PatientGrain grain =
                jdbc.queryForObject(
                        sql.toString(),
                        (rs, n) ->
                                new PatientGrain(
                                        rs.getLong("all_count"),
                                        rs.getLong("new_count"),
                                        rs.getLong("male"),
                                        rs.getLong("female")),
                        args.toArray());

        var bySex = new LinkedHashMap<String, Long>();
        bySex.put("M", grain.male());
        bySex.put("F", grain.female());

        var catSql = new StringBuilder(PATIENTS_BY_CATEGORY_SQL);
        var catArgs = new ArrayList<Object>(List.of(f, tx));
        if (insuranceId != null) {
            catSql.append(" AND lf.insurance_id = ?");
            catArgs.add(insuranceId);
        }
        if (partnerId != null) {
            catSql.append(" AND lf.partner_id = ?");
            catArgs.add(partnerId);
        }
        catSql.append(" GROUP BY r.patient_category_snapshot");
        var byAgeBand = new LinkedHashMap<String, Long>();
        jdbc.query(
                catSql.toString(),
                rs -> {
                    String category = rs.getString("category");
                    if (category != null) {
                        byAgeBand.put(category, rs.getLong("cnt"));
                    }
                },
                catArgs.toArray());

        return new PatientStatsRow(grain.allCount(), grain.newCount(), bySex, byAgeBand);
    }
}
