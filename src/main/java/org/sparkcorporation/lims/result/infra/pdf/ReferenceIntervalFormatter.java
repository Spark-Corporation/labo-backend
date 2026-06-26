package org.sparkcorporation.lims.result.infra.pdf;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.ReferenceIntervalSummary;

/**
 * Pure formatter rendering a reference interval to its printed display string (CDC §IV / §Unités).
 * Extracted from SP7's {@code PaillasseWorksheetModelAssembler#formatInterval} so both the
 * worksheet and the Compte-Rendu share one implementation, with a unit-2 variant added for SP11.
 * STANDARD mode renders {@code "min – max"} (en-dash) from the stored bounds; COMMENT mode renders
 * the comment text (unit-1 only); NONE / absent bounds render {@code ""}.
 */
public final class ReferenceIntervalFormatter {

    private ReferenceIntervalFormatter() {
        throw new AssertionError("utility class");
    }

    /** Unit-1 reference: {@code "min – max"} | comment text | {@code ""}. */
    public static String unit1(@Nullable ReferenceIntervalSummary ri) {
        if (ri == null) {
            return "";
        }
        if (ri.unit1Min() != null && ri.unit1Max() != null) {
            return "%s – %s"
                    .formatted(ri.unit1Min().toPlainString(), ri.unit1Max().toPlainString());
        }
        return ri.commentText() != null ? ri.commentText() : "";
    }

    /** Unit-2 reference from the stored unit-2 bounds: {@code "min – max"} | {@code ""}. */
    public static String unit2(@Nullable ReferenceIntervalSummary ri) {
        if (ri == null) {
            return "";
        }
        if (ri.unit2Min() != null && ri.unit2Max() != null) {
            return "%s – %s"
                    .formatted(ri.unit2Min().toPlainString(), ri.unit2Max().toPlainString());
        }
        return "";
    }
}
