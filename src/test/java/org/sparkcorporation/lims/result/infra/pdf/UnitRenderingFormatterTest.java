package org.sparkcorporation.lims.result.infra.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.ReferenceIntervalId;
import org.sparkcorporation.lims.catalog.api.ReferenceIntervalSummary;
import org.sparkcorporation.lims.catalog.api.ReferenceMode;
import org.sparkcorporation.lims.shared.domain.PatientCategory;

class UnitRenderingFormatterTest {

    // ---- UnitValueFormatter ----

    @Test
    void unit1_value_formats_to_configured_decimals() {
        assertThat(UnitValueFormatter.unit1Value(new BigDecimal("1.3"), 2)).isEqualTo("1.30");
        assertThat(UnitValueFormatter.unit1Value(new BigDecimal("1.005"), 2)).isEqualTo("1.01");
        assertThat(UnitValueFormatter.unit1Value(null, 2)).isEmpty();
        assertThat(UnitValueFormatter.unit1Value(new BigDecimal("2.5"), null)).isEqualTo("2.5");
    }

    @Test
    void unit2_value_applies_factor_and_decimals() {
        assertThat(UnitValueFormatter.unit2Value(new BigDecimal("0.5"), new BigDecimal("18"), 1))
                .isEqualTo("9.0");
        assertThat(UnitValueFormatter.unit2Value(new BigDecimal("0.5"), null, 1)).isNull();
        assertThat(UnitValueFormatter.unit2Value(null, new BigDecimal("18"), 1)).isNull();
    }

    // ---- ReferenceIntervalFormatter ----

    private static ReferenceIntervalSummary ri(
            ReferenceMode mode,
            String u1Min,
            String u1Max,
            String u2Min,
            String u2Max,
            String comment) {
        return new ReferenceIntervalSummary(
                new ReferenceIntervalId("01KQZ0WPVQA364NMRAJMKRQC01"),
                new AnalysisId("01KQZ0WPVQA364NMRAJMKRQC02"),
                PatientCategory.HOMME,
                null,
                null,
                null,
                null,
                mode,
                u1Min == null ? null : new BigDecimal(u1Min),
                u1Max == null ? null : new BigDecimal(u1Max),
                u2Min == null ? null : new BigDecimal(u2Min),
                u2Max == null ? null : new BigDecimal(u2Max),
                comment);
    }

    @Test
    void reference_unit1_standard_renders_min_dash_max() {
        var r = ri(ReferenceMode.STANDARD, "0.70", "1.10", "3.9", "6.1", null);
        assertThat(ReferenceIntervalFormatter.unit1(r)).isEqualTo("0.70 – 1.10");
        assertThat(ReferenceIntervalFormatter.unit2(r)).isEqualTo("3.9 – 6.1");
    }

    @Test
    void reference_comment_mode_renders_comment_for_unit1_and_blank_unit2() {
        var r = ri(ReferenceMode.COMMENT, null, null, null, null, "Voir clinique");
        assertThat(ReferenceIntervalFormatter.unit1(r)).isEqualTo("Voir clinique");
        assertThat(ReferenceIntervalFormatter.unit2(r)).isEmpty();
    }

    @Test
    void reference_null_yields_empty() {
        assertThat(ReferenceIntervalFormatter.unit1(null)).isEmpty();
        assertThat(ReferenceIntervalFormatter.unit2(null)).isEmpty();
    }
}
