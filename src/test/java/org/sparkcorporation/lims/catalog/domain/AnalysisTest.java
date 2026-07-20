package org.sparkcorporation.lims.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.AnalysisCode;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.DisciplineId;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.shared.id.Ulid;

class AnalysisTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-07T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void convert_to_simple_rejects_grouped_with_non_empty_components() {
        Analysis grouped = stubGroupedAnalysisWithOneComponent();
        assertThatThrownBy(grouped::convertToSimple)
                .isInstanceOf(HasComponentsException.class)
                .hasMessageContaining("components");
    }

    @Test
    void add_component_rejects_when_parent_is_simple() {
        var parent = stubAnalysis("PARENT", AnalysisKind.SIMPLE);
        var child = stubAnalysis("CHILD", AnalysisKind.SIMPLE);
        assertThatThrownBy(() -> parent.addComponent(child, 1))
                .isInstanceOf(KindAlreadyMatchesException.class)
                .hasMessageContaining("SIMPLE");
    }

    @Test
    void add_component_rejects_when_child_is_grouped() {
        var parent = stubAnalysis("PARENT", AnalysisKind.GROUPED);
        var child = stubAnalysis("CHILD", AnalysisKind.GROUPED);
        assertThatThrownBy(() -> parent.addComponent(child, 1))
                .isInstanceOf(ChildMustBeSimpleException.class);
    }

    @Test
    void add_component_rejects_when_child_is_society_panel() {
        var parent = stubAnalysis("PARENT", AnalysisKind.GROUPED);
        var child = stubAnalysis("CHILD", AnalysisKind.SOCIETY_PANEL);
        assertThatThrownBy(() -> parent.addComponent(child, 1))
                .isInstanceOf(ChildMustBeSimpleException.class);
    }

    @Test
    void add_component_rejects_self_loop() {
        var a = stubAnalysis("SELF", AnalysisKind.GROUPED);
        assertThatThrownBy(() -> a.addComponent(a, 1))
                .isInstanceOf(ChildMustBeSimpleException.class)
                .hasMessageContaining("self");
    }

    @Test
    void convert_to_grouped_rejects_when_already_grouped() {
        var a = stubAnalysis("GRP", AnalysisKind.GROUPED);
        assertThatThrownBy(a::convertToGrouped).isInstanceOf(KindAlreadyMatchesException.class);
    }

    @Test
    void convert_to_grouped_rejects_when_already_society_panel() {
        var a = stubAnalysis("BIL", AnalysisKind.SOCIETY_PANEL);
        assertThatThrownBy(a::convertToGrouped).isInstanceOf(KindAlreadyMatchesException.class);
    }

    @Test
    void convert_to_simple_rejects_when_already_simple() {
        var a = stubAnalysis("SMP", AnalysisKind.SIMPLE);
        assertThatThrownBy(a::convertToSimple).isInstanceOf(KindAlreadyMatchesException.class);
    }

    @Test
    void convert_to_simple_succeeds_when_components_empty() {
        var a = stubAnalysis("GRP", AnalysisKind.GROUPED);
        a.convertToSimple();
        assertThat(a.kind()).isEqualTo(AnalysisKind.SIMPLE);
    }

    @Test
    void duplicate_clones_components_with_fresh_parent_id_same_child_ids() {
        var grouped = stubGroupedAnalysisWithOneComponent();
        var originalChildId = grouped.components().get(0).childId();

        var clone = grouped.duplicate(AnalysisCode.of("DUP_GRP"), FIXED);

        assertThat(clone.id().value()).isNotEqualTo(grouped.id().value());
        assertThat(clone.components()).hasSize(1);
        assertThat(clone.components().get(0).parentId()).isEqualTo(clone.id().value());
        assertThat(clone.components().get(0).childId()).isEqualTo(originalChildId);
    }

    @Test
    void duplicate_clones_techniques_with_fresh_analysis_id() {
        var source = stubAnalysis("SRC", AnalysisKind.SIMPLE);
        var techniqueId = org.sparkcorporation.lims.catalog.api.TechniqueId.next();
        source.techniques().add(new AnalysisTechniqueLink(source.id(), techniqueId, true));

        var clone = source.duplicate(AnalysisCode.of("DUP_SRC"), FIXED);

        assertThat(clone.techniques()).hasSize(1);
        var techLink = clone.techniques().iterator().next();
        assertThat(techLink.analysisId()).isEqualTo(clone.id().value());
        assertThat(techLink.techniqueId()).isEqualTo(techniqueId.value());
        assertThat(techLink.isDefault()).isTrue();
    }

    @Test
    void duplicate_appends_copy_suffix_to_name() {
        var source = stubAnalysis("SRC", AnalysisKind.SIMPLE);
        var clone = source.duplicate(AnalysisCode.of("DUP_SRC"), FIXED);
        assertThat(clone.name()).isEqualTo(source.name() + " (copy)");
    }

    @Test
    void duplicate_assigns_new_ulid_id_and_uses_new_code() {
        var source = stubAnalysis("SRC", AnalysisKind.SIMPLE);
        var clone = source.duplicate(AnalysisCode.of("DUP_SRC"), FIXED);
        assertThat(Ulid.isValid(clone.id().value())).isTrue();
        assertThat(clone.id().value()).isNotEqualTo(source.id().value());
        assertThat(clone.code()).isEqualTo("DUP_SRC");
    }

    @Test
    void is_active_true_when_not_deleted() {
        var a = stubAnalysis("ACT", AnalysisKind.SIMPLE);
        assertThat(a.isActive()).isTrue();
        assertThat(a.deletedAt()).isNull();
    }

    @Test
    void create_carries_formula_expression() {
        var a = formulaAnalysis("LDL", "CT - HDL - TG/5");
        assertThat(a.formulaExpression()).isEqualTo("CT - HDL - TG/5");
    }

    @Test
    void duplicate_clones_formula_expression() {
        var source = formulaAnalysis("LDL", "CT - HDL - TG/5");
        var clone = source.duplicate(AnalysisCode.of("DUP_LDL"), FIXED);
        assertThat(clone.formulaExpression()).isEqualTo("CT - HDL - TG/5");
    }

    private static Analysis formulaAnalysis(String code, String expression) {
        var cmd =
                new NewAnalysis(
                        code,
                        "Stub " + code,
                        AnalysisKind.SIMPLE,
                        new BigDecimal("10.00"),
                        1,
                        DisciplineId.next(),
                        null,
                        null,
                        ResultType.FORMULA,
                        null,
                        expression,
                        null,
                        null,
                        null,
                        null);
        return Analysis.create(cmd, null, null, null, FIXED);
    }

    private static Analysis stubAnalysis(String code, AnalysisKind kind) {
        var cmd =
                new NewAnalysis(
                        code,
                        "Stub " + code,
                        kind,
                        new BigDecimal("10.00"),
                        1,
                        DisciplineId.next(),
                        null,
                        null,
                        ResultType.REAL,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        return Analysis.create(
                cmd, /* discipline */ null, /* matrix */ null, /* recipient */ null, FIXED);
    }

    private static Analysis stubGroupedAnalysisWithOneComponent() {
        var parent = stubAnalysis("GRP_PARENT", AnalysisKind.GROUPED);
        var child = stubAnalysis("SMP_CHILD", AnalysisKind.SIMPLE);
        parent.addComponent(child, 1);
        return parent;
    }
}
