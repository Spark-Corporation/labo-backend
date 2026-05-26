package org.sparkcorporation.lims.iam.domain;

import java.util.Set;

/**
 * Tech doc §9.5 permission catalog. Static set of 46 strings. Used as the source of truth for
 * {@code @RequiresPermission(...)} keys; an integrity test at startup verifies this set matches the
 * DB-seeded {@code permissions.name} column set.
 *
 * <p>{@code cash-register.manage} was added in 1.6.a chunk 6 for the new {@code
 * /api/v1/cash-registers/*} surface; seeded via {@code 202605120821-seed-cash-register-permission}.
 *
 * <p>{@code accounting.read} was added in 1.6.a chunk 8 for the new {@code /api/v1/accounting/*}
 * read-side surface (CDC §VIII recap / by-mode / outstanding); seeded via {@code
 * 202605120850-seed-accounting-permission}.
 *
 * <p>The 9 {@code cashbox.*} permissions (expense-category.{read,manage}, expense.{read,record},
 * home-collection.{read,record}, journal.read, session.{close,reopen}) were added in 1.7.a chunk 2
 * for the new {@code /api/v1/cashbox/*} surface; seeded via {@code
 * 202605180930-seed-cashbox-permissions}.
 *
 * <p>{@code pdf.read-own-jobs} was added in 1.5.c chunk 5 for the new {@code /api/v1/pdf-jobs/*}
 * polling surface; seeded via {@code 202605200905-seed-pdf-permission} (granted to every role
 * already holding {@code lab-file.read}).
 *
 * <p>{@code lab-settings.read} and {@code lab-settings.manage} were added in 1.5.e chunk 1 for the
 * new {@code /api/v1/lab-settings} admin surface; seeded via {@code
 * 202605210905-seed-lab-settings-permissions} (read granted to every role already holding {@code
 * lab-file.read}; manage granted to the {@code ADMIN} role only — {@code ROLE_} is the secured-x
 * runtime authority prefix, not the persisted role name).
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
                    "accounting.read",
                    "cashbox.expense-category.read",
                    "cashbox.expense-category.manage",
                    "cashbox.expense.read",
                    "cashbox.expense.record",
                    "cashbox.home-collection.read",
                    "cashbox.home-collection.record",
                    "cashbox.journal.read",
                    "cashbox.session.close",
                    "cashbox.session.reopen",
                    "pdf.read-own-jobs",
                    "lab-settings.read",
                    "lab-settings.manage");

    private PermissionCatalog() {}
}
