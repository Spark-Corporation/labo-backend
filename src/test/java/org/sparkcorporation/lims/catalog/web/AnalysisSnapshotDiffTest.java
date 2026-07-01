package org.sparkcorporation.lims.catalog.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class AnalysisSnapshotDiffTest {

    private static AnalysisSnapshot base() {
        return new AnalysisSnapshot(
                "Glycémie",
                new BigDecimal("10.00"),
                1,
                "REAL",
                "SIMPLE",
                null,
                null,
                null,
                null,
                null,
                null,
                "g/L",
                2,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private static AnalysisSnapshot from(
            AnalysisSnapshot p, String name, String kind, String unit1) {
        return new AnalysisSnapshot(
                name,
                p.indiciaryValueB(),
                p.labelCount(),
                p.resultType(),
                kind,
                p.interpretationText(),
                p.formulaExpression(),
                p.defaultPortoir(),
                p.defaultConservationDays(),
                p.matrixId(),
                p.recipientId(),
                unit1,
                p.unit1Decimals(),
                p.unit2(),
                p.unit2Decimals(),
                p.conversionFactor(),
                p.delayDays(),
                p.delayHours(),
                p.anteriorityCount());
    }

    @Test
    void identical_snapshots_have_no_changed_fields() {
        assertThat(AnalysisSnapshotDiff.diff(base(), base())).isEmpty();
    }

    @Test
    void big_decimal_compares_by_value_not_scale() {
        AnalysisSnapshot prev = base();
        AnalysisSnapshot curr =
                new AnalysisSnapshot(
                        prev.name(),
                        new BigDecimal("10.000"),
                        prev.labelCount(),
                        prev.resultType(),
                        prev.kind(),
                        prev.interpretationText(),
                        prev.formulaExpression(),
                        prev.defaultPortoir(),
                        prev.defaultConservationDays(),
                        prev.matrixId(),
                        prev.recipientId(),
                        prev.unit1(),
                        prev.unit1Decimals(),
                        prev.unit2(),
                        prev.unit2Decimals(),
                        prev.conversionFactor(),
                        prev.delayDays(),
                        prev.delayHours(),
                        prev.anteriorityCount());
        assertThat(AnalysisSnapshotDiff.diff(prev, curr)).isEmpty();
    }

    @Test
    void detects_scalar_field_change() {
        AnalysisSnapshot prev = base();
        AnalysisSnapshot curr = from(prev, "Glycémie à jeun", prev.kind(), prev.unit1());
        assertThat(AnalysisSnapshotDiff.diff(prev, curr)).containsExactly("name");
    }

    @Test
    void detects_kind_change() {
        AnalysisSnapshot prev = base();
        AnalysisSnapshot curr = from(prev, prev.name(), "GROUPED", prev.unit1());
        assertThat(AnalysisSnapshotDiff.diff(prev, curr)).containsExactly("kind");
    }

    @Test
    void detects_embedded_units_subfield_change() {
        AnalysisSnapshot prev = base();
        AnalysisSnapshot curr = from(prev, prev.name(), prev.kind(), "mg/L");
        assertThat(AnalysisSnapshotDiff.diff(prev, curr)).containsExactly("units.unit1");
    }

    @Test
    void detects_multiple_changed_fields() {
        AnalysisSnapshot prev = base();
        AnalysisSnapshot curr =
                new AnalysisSnapshot(
                        "Nouvelle",
                        prev.indiciaryValueB(),
                        prev.labelCount(),
                        prev.resultType(),
                        prev.kind(),
                        "Interprétation",
                        prev.formulaExpression(),
                        prev.defaultPortoir(),
                        prev.defaultConservationDays(),
                        prev.matrixId(),
                        prev.recipientId(),
                        prev.unit1(),
                        prev.unit1Decimals(),
                        prev.unit2(),
                        prev.unit2Decimals(),
                        prev.conversionFactor(),
                        prev.delayDays(),
                        prev.delayHours(),
                        prev.anteriorityCount());
        assertThat(AnalysisSnapshotDiff.diff(prev, curr))
                .containsExactly("name", "interpretationText");
    }
}
