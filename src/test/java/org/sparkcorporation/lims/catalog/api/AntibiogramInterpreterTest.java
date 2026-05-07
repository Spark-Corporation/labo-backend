package org.sparkcorporation.lims.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.shared.domain.Sensitivity;

class AntibiogramInterpreterTest {

    @Test
    void interpret_returns_R_when_measured_below_reference_diameter() {
        var ab = stubAntibioticSummary(new BigDecimal("20.0"), null, null);
        assertThat(AntibiogramInterpreter.interpret(new BigDecimal("15.0"), ab))
                .isEqualTo(Sensitivity.R);
    }

    @Test
    void interpret_returns_S_when_measured_at_or_above_reference_with_no_intermediate_bounds() {
        var ab = stubAntibioticSummary(new BigDecimal("20.0"), null, null);
        assertThat(AntibiogramInterpreter.interpret(new BigDecimal("20.0"), ab))
                .isEqualTo(Sensitivity.S);
        assertThat(AntibiogramInterpreter.interpret(new BigDecimal("25.0"), ab))
                .isEqualTo(Sensitivity.S);
    }

    @Test
    void interpret_returns_I_when_measured_within_intermediate_bounds_inclusive() {
        var ab =
                stubAntibioticSummary(
                        new BigDecimal("20.0"), new BigDecimal("17.0"), new BigDecimal("22.0"));
        assertThat(AntibiogramInterpreter.interpret(new BigDecimal("17.0"), ab))
                .isEqualTo(Sensitivity.I);
        assertThat(AntibiogramInterpreter.interpret(new BigDecimal("19.5"), ab))
                .isEqualTo(Sensitivity.I);
        assertThat(AntibiogramInterpreter.interpret(new BigDecimal("22.0"), ab))
                .isEqualTo(Sensitivity.I);
    }

    @Test
    void interpret_returns_R_when_measured_below_intermediate_lower() {
        var ab =
                stubAntibioticSummary(
                        new BigDecimal("20.0"), new BigDecimal("17.0"), new BigDecimal("22.0"));
        assertThat(AntibiogramInterpreter.interpret(new BigDecimal("16.0"), ab))
                .isEqualTo(Sensitivity.R);
    }

    @Test
    void interpret_returns_S_when_measured_above_intermediate_upper() {
        var ab =
                stubAntibioticSummary(
                        new BigDecimal("20.0"), new BigDecimal("17.0"), new BigDecimal("22.0"));
        assertThat(AntibiogramInterpreter.interpret(new BigDecimal("23.0"), ab))
                .isEqualTo(Sensitivity.S);
    }

    @Test
    void interpret_throws_npe_on_null_measured() {
        var ab = stubAntibioticSummary(new BigDecimal("20.0"), null, null);
        assertThatThrownBy(() -> AntibiogramInterpreter.interpret(null, ab))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void interpret_throws_npe_on_null_antibiotic() {
        assertThatThrownBy(() -> AntibiogramInterpreter.interpret(new BigDecimal("20"), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void interpret_with_only_lower_intermediate_falls_through_to_strict_compare() {
        // single intermediate bound shouldn't trigger I; both lo and hi must be non-null
        var ab = stubAntibioticSummary(new BigDecimal("20.0"), new BigDecimal("17.0"), null);
        assertThat(AntibiogramInterpreter.interpret(new BigDecimal("18.0"), ab))
                .isEqualTo(
                        Sensitivity.R); // 18 < 20 → R (intermediate range ignored — only one bound)
    }

    private static AntibioticSummary stubAntibioticSummary(
            BigDecimal ref, BigDecimal lo, BigDecimal hi) {
        return new AntibioticSummary(
                AntibioticId.next(),
                "PEN",
                "Penicillin",
                "Beta-lactam antibiotic",
                "Beta-lactam",
                ref,
                lo,
                hi,
                true);
    }
}
