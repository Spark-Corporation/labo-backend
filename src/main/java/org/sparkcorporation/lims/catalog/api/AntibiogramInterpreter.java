package org.sparkcorporation.lims.catalog.api;

import java.math.BigDecimal;
import java.util.Objects;

import org.sparkcorporation.lims.shared.domain.Sensitivity;

/**
 * Pure-function antibiogram interpretation per CDC §6 + tech doc Appendix A.3.
 *
 * <p>Intermediate bounds are inclusive ({@code measured ∈ [lo, hi]}) per tech doc precedent and
 * clinical microbiology convention — boundary disk-diameter measurements are classified
 * "Intermediate" rather than the strict CDC {@code X1 < m < X2} interpretation. See spec §5.4 for
 * the boundary-semantics judgment-call rationale.
 *
 * <p>No DB, no Spring. Public for cross-module consumption (1.5 result module).
 */
public final class AntibiogramInterpreter {

    private AntibiogramInterpreter() {
        throw new AssertionError("utility class");
    }

    public static Sensitivity interpret(BigDecimal measured, AntibioticSummary antibiotic) {
        Objects.requireNonNull(measured, "measured");
        Objects.requireNonNull(antibiotic, "antibiotic");
        var ref = antibiotic.referenceDiameter();
        var lo = antibiotic.intermediateLower();
        var hi = antibiotic.intermediateUpper();
        if (lo != null
                && hi != null
                && measured.compareTo(lo) >= 0
                && measured.compareTo(hi) <= 0) {
            return Sensitivity.I;
        }
        return measured.compareTo(ref) < 0 ? Sensitivity.R : Sensitivity.S;
    }
}
