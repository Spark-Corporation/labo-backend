package org.sparkcorporation.lims.iam.domain;

import java.util.Set;

/**
 * Tech doc §9.5 permission catalog. Static set of 32 strings. Used as the source of truth for
 * {@code @RequiresPermission(...)} keys; an integrity test at startup verifies this set matches the
 * DB-seeded {@code permissions.name} column set.
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
                    "users.manage",
                    "stats.read",
                    "audit.read");

    private PermissionCatalog() {}
}
