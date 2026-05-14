package org.sparkcorporation.lims.iam.domain;

import java.util.Set;

/**
 * Tech doc §9.5 permission catalog. Static set of 34 strings. Used as the source of truth for
 * {@code @RequiresPermission(...)} keys; an integrity test at startup verifies this set matches the
 * DB-seeded {@code permissions.name} column set.
 *
 * <p>{@code cash-register.manage} was added in 1.6.a chunk 6 for the new {@code
 * /api/v1/cash-registers/*} surface; seeded via {@code 202605120821-seed-cash-register-permission}.
 *
 * <p>{@code accounting.read} was added in 1.6.a chunk 8 for the new {@code /api/v1/accounting/*}
 * read-side surface (CDC §VIII recap / by-mode / outstanding); seeded via {@code
 * 202605120850-seed-accounting-permission}.
 */
public final class PermissionCatalog {

    public static final Set<String> ALL =
            Set.of(
                    "patient.create",
                    "patient.update",
                    "patient.search",
                    "patient.merge",
                    "lab-file.create",
                    "lab-file.read",
                    "sample.record-in-lab",
                    "sample.record-external",
                    "sample.record-home",
                    "result.enter",
                    "result.validate-tech",
                    "result.validate-bio",
                    "dossier.sign-electronically",
                    "dossier.publish",
                    "quote.create",
                    "quote.send",
                    "quote.lock",
                    "quote.read",
                    "invoice.create",
                    "invoice.read",
                    "payment.collect",
                    "expense.record",
                    "cashbox.use-assigned",
                    "cashbox.close-day",
                    "home-collection-fee.record",
                    "catalog.manage",
                    "prescriber.manage",
                    "insurance.manage",
                    "partner.manage",
                    "cash-register.manage",
                    "users.manage",
                    "stats.read",
                    "audit.read",
                    "accounting.read");

    private PermissionCatalog() {}
}
