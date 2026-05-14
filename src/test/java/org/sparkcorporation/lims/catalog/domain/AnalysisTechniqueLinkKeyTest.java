package org.sparkcorporation.lims.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Unit tests for {@link AnalysisTechniqueLinkKey}'s equals/hashCode contract. Covers the four
 * equals-branches JaCoCo otherwise flags as missed.
 */
class AnalysisTechniqueLinkKeyTest {

    private final String analysis = Ulid.generate();
    private final String technique = Ulid.generate();

    @Test
    void equals_returns_true_for_same_reference() {
        var k = new AnalysisTechniqueLinkKey(analysis, technique);
        assertThat(k).isEqualTo(k);
    }

    @Test
    void equals_returns_true_for_same_components() {
        var a = new AnalysisTechniqueLinkKey(analysis, technique);
        var b = new AnalysisTechniqueLinkKey(analysis, technique);
        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }

    @Test
    void equals_returns_false_for_different_analysisId() {
        var a = new AnalysisTechniqueLinkKey(analysis, technique);
        var b = new AnalysisTechniqueLinkKey(Ulid.generate(), technique);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returns_false_for_different_techniqueId() {
        var a = new AnalysisTechniqueLinkKey(analysis, technique);
        var b = new AnalysisTechniqueLinkKey(analysis, Ulid.generate());
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returns_false_for_null_argument() {
        var k = new AnalysisTechniqueLinkKey(analysis, technique);
        assertThat(k).isNotEqualTo(null);
    }

    @Test
    void equals_returns_false_for_different_type() {
        var k = new AnalysisTechniqueLinkKey(analysis, technique);
        assertThat(k).isNotEqualTo("not a key");
    }

    @Test
    void no_args_constructor_then_setters_populate_state() {
        var k = new AnalysisTechniqueLinkKey();
        k.setAnalysisId(analysis);
        k.setTechniqueId(technique);
        assertThat(k.getAnalysisId()).isEqualTo(analysis);
        assertThat(k.getTechniqueId()).isEqualTo(technique);
    }

    @Test
    void accessors_expose_constructor_values() {
        var k = new AnalysisTechniqueLinkKey(analysis, technique);
        assertThat(k.getAnalysisId()).isEqualTo(analysis);
        assertThat(k.getTechniqueId()).isEqualTo(technique);
    }
}
