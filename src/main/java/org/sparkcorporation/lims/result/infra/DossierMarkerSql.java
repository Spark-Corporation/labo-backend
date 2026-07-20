package org.sparkcorporation.lims.result.infra;

/**
 * Reusable SQL fragments for dossier-list and dossier-queue native queries, per spec §6.3.
 *
 * <p>The fragments are compile-time string constants concatenated into the FINAL native
 * {@code @Query} value at repository construction. No user input is interpolated into these
 * fragments — the WHERE clauses that vary by request bind through named parameters, so there is no
 * SQL injection surface.
 *
 * <p><b>Schema-coupling note:</b> the fragments reference {@code results.state}, {@code
 * results.deleted_at}, {@code result_lines.is_out_of_range}, and {@code lab_files.id}. Schema
 * changes to any of those (column renames, soft-delete strategy changes) require a coordinated
 * update here and in {@code JpaDossierRepositoryTest}.
 */
public final class DossierMarkerSql {

    private DossierMarkerSql() {
        throw new AssertionError("utility class");
    }

    /**
     * Per-row marker projection — derives {@code GREEN | RED | PEN} from the result rows of a
     * lab-file. PEN takes precedence over RED (partially-filled outranks abnormal); RED over GREEN.
     *
     * <p>Use as a SELECT-clause column expression. Assumes the FROM clause aliases {@code
     * lab_files} as {@code lf}.
     */
    public static final String MARKER_CASE =
            """
            CASE
              WHEN EXISTS (SELECT 1 FROM results r2
                           WHERE r2.lab_file_id = lf.id
                             AND r2.deleted_at IS NULL
                             AND r2.state = 'DRAFT') THEN 'PEN'
              WHEN EXISTS (SELECT 1 FROM result_lines rl
                           JOIN results r3 ON r3.id = rl.result_id
                           WHERE r3.lab_file_id = lf.id
                             AND r3.deleted_at IS NULL
                             AND rl.is_out_of_range = true) THEN 'RED'
              ELSE 'GREEN'
            END
            """;

    /**
     * "Non encore rendus" predicate per CDC §X line 255 — a dossier qualifies if at least one
     * active result is NOT {@code BIOLOGICAL_VALIDATED}. Use as a WHERE-clause boolean predicate.
     */
    public static final String NON_RENDU_PREDICATE =
            """
            EXISTS (
              SELECT 1 FROM results r4
              WHERE r4.lab_file_id = lf.id
                AND r4.deleted_at IS NULL
                AND r4.state <> 'BIOLOGICAL_VALIDATED'
            )
            """;
}
