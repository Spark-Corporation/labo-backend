package org.sparkcorporation.lims.billing.infra;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

/**
 * Read-side projections for CDC §VIII accounting endpoints (spec §11.2, plan chunk 8). Hosts three
 * native SQL queries against the chunk-5/6/7 schema:
 *
 * <ul>
 *   <li>{@link #daily(LocalDate)} — recap grouped per lab-file on a single calendar day.
 *   <li>{@link #byPaymentMode(Instant, Instant)} — payment totals grouped by {@code mode} over a
 *       half-open time window.
 *   <li>{@link #outstanding()} — current open balance split into {@code unpaid} (paid &lt; total)
 *       and {@code refundsOwed} (paid &gt; total).
 * </ul>
 *
 * <p>Raw {@link JdbcTemplate} is used deliberately — these are pure read projections with no
 * domain-entity mapping or transactional write semantics, and writing the SQL out longhand keeps
 * the query plan obvious for the DBA.
 */
@Component
@RequiredArgsConstructor
public class AccountingReadModel {

    private static final String DAILY_SQL =
            """
            SELECT
                lf.id AS lab_file_id,
                lf.registration_number AS registration_number,
                COALESCE(inv.customer_name_snapshot, p.lab_code) AS customer_name,
                COALESCE(inv.total_amount, 0) AS total_amount,
                COALESCE(pay.paid_amount, 0) AS paid_amount,
                (COALESCE(inv.total_amount, 0) - COALESCE(pay.paid_amount, 0)) AS remaining_amount
            FROM lab_files lf
            JOIN patients p ON p.id = lf.patient_id
            LEFT JOIN invoices inv ON inv.lab_file_id = lf.id
            LEFT JOIN (
                SELECT lab_file_id, SUM(amount) AS paid_amount
                FROM payments
                WHERE (recorded_at AT TIME ZONE 'UTC')::date = ?
                GROUP BY lab_file_id
            ) pay ON pay.lab_file_id = lf.id
            WHERE
                (inv.id IS NOT NULL AND (inv.issued_at AT TIME ZONE 'UTC')::date = ?)
                OR pay.paid_amount IS NOT NULL
            ORDER BY lf.created_at
            """;

    private static final String BY_MODE_SQL =
            """
            SELECT mode, SUM(amount) AS total_amount, COUNT(*) AS payment_count
            FROM payments
            WHERE recorded_at >= ? AND recorded_at < ?
            GROUP BY mode
            ORDER BY mode
            """;

    private static final String UNPAID_SQL =
            """
            SELECT
                lf.id AS lab_file_id,
                lf.registration_number AS registration_number,
                COALESCE(inv.customer_name_snapshot, p.lab_code) AS customer_name,
                inv.total_amount AS total_amount,
                COALESCE(pay.paid_amount, 0) AS paid_amount,
                (inv.total_amount - COALESCE(pay.paid_amount, 0)) AS remaining_amount
            FROM invoices inv
            JOIN lab_files lf ON inv.lab_file_id = lf.id
            JOIN patients p ON p.id = lf.patient_id
            LEFT JOIN (
                SELECT lab_file_id, SUM(amount) AS paid_amount
                FROM payments
                GROUP BY lab_file_id
            ) pay ON pay.lab_file_id = lf.id
            WHERE COALESCE(pay.paid_amount, 0) < inv.total_amount
            ORDER BY lf.created_at
            """;

    private static final String REFUNDS_OWED_SQL =
            """
            SELECT
                lf.id AS lab_file_id,
                lf.registration_number AS registration_number,
                COALESCE(inv.customer_name_snapshot, p.lab_code) AS customer_name,
                inv.total_amount AS total_amount,
                COALESCE(pay.paid_amount, 0) AS paid_amount,
                (COALESCE(pay.paid_amount, 0) - inv.total_amount) AS remaining_amount
            FROM invoices inv
            JOIN lab_files lf ON inv.lab_file_id = lf.id
            JOIN patients p ON p.id = lf.patient_id
            LEFT JOIN (
                SELECT lab_file_id, SUM(amount) AS paid_amount
                FROM payments
                GROUP BY lab_file_id
            ) pay ON pay.lab_file_id = lf.id
            WHERE COALESCE(pay.paid_amount, 0) > inv.total_amount
            ORDER BY lf.created_at
            """;

    private static final RowMapper<DailyRow> DAILY_MAPPER =
            (rs, n) ->
                    new DailyRow(
                            rs.getString("lab_file_id"),
                            rs.getString("registration_number"),
                            rs.getString("customer_name"),
                            rs.getBigDecimal("total_amount"),
                            rs.getBigDecimal("paid_amount"),
                            rs.getBigDecimal("remaining_amount"));

    private static final RowMapper<ByModeRow> BY_MODE_MAPPER =
            (rs, n) ->
                    new ByModeRow(
                            rs.getString("mode"),
                            rs.getBigDecimal("total_amount"),
                            rs.getLong("payment_count"));

    private static final RowMapper<OutstandingRow> OUTSTANDING_MAPPER =
            (rs, n) ->
                    new OutstandingRow(
                            rs.getString("lab_file_id"),
                            rs.getString("registration_number"),
                            rs.getString("customer_name"),
                            rs.getBigDecimal("total_amount"),
                            rs.getBigDecimal("paid_amount"),
                            rs.getBigDecimal("remaining_amount"));

    private final JdbcTemplate jdbc;

    /**
     * Daily recap for {@code date}: one entry per lab-file whose invoice was issued on the given
     * day or which received any payment that day. Lab files with no billing activity are excluded.
     */
    public List<DailyRow> daily(LocalDate date) {
        var sqlDate = Date.valueOf(date);
        return jdbc.query(DAILY_SQL, DAILY_MAPPER, sqlDate, sqlDate);
    }

    /** Payment totals grouped by {@code mode} over the half-open window {@code [from, to)}. */
    public List<ByModeRow> byPaymentMode(Instant from, Instant to) {
        return jdbc.query(BY_MODE_SQL, BY_MODE_MAPPER, Timestamp.from(from), Timestamp.from(to));
    }

    /**
     * Open ledger: unpaid invoices (customer still owes) and refunds owed (clinic still owes), as
     * of the current moment. Excludes fully-paid invoices.
     */
    public OutstandingResult outstanding() {
        var unpaid = jdbc.query(UNPAID_SQL, OUTSTANDING_MAPPER);
        var refundsOwed = jdbc.query(REFUNDS_OWED_SQL, OUTSTANDING_MAPPER);
        return new OutstandingResult(unpaid, refundsOwed);
    }
}
