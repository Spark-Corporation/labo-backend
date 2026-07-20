package org.sparkcorporation.lims.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Unit tests for {@link AnalysisComponentKey}'s equals/hashCode contract. Covers the four
 * equals-branches JaCoCo otherwise flags as missed.
 */
class AnalysisComponentKeyTest {

    private final String parent = Ulid.generate();
    private final String child = Ulid.generate();

    @Test
    void equals_returns_true_for_same_reference() {
        var k = new AnalysisComponentKey(parent, child);
        assertThat(k).isEqualTo(k);
    }

    @Test
    void equals_returns_true_for_same_components() {
        var a = new AnalysisComponentKey(parent, child);
        var b = new AnalysisComponentKey(parent, child);
        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }

    @Test
    void equals_returns_false_for_different_parentId() {
        var a = new AnalysisComponentKey(parent, child);
        var b = new AnalysisComponentKey(Ulid.generate(), child);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returns_false_for_different_childId() {
        var a = new AnalysisComponentKey(parent, child);
        var b = new AnalysisComponentKey(parent, Ulid.generate());
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returns_false_for_null_argument() {
        var k = new AnalysisComponentKey(parent, child);
        assertThat(k).isNotEqualTo(null);
    }

    @Test
    void equals_returns_false_for_different_type() {
        var k = new AnalysisComponentKey(parent, child);
        assertThat(k).isNotEqualTo("not a key");
    }

    @Test
    void no_args_constructor_then_setters_populate_state() {
        var k = new AnalysisComponentKey();
        k.setParentId(parent);
        k.setChildId(child);
        assertThat(k.getParentId()).isEqualTo(parent);
        assertThat(k.getChildId()).isEqualTo(child);
    }

    @Test
    void accessors_expose_constructor_values() {
        var k = new AnalysisComponentKey(parent, child);
        assertThat(k.getParentId()).isEqualTo(parent);
        assertThat(k.getChildId()).isEqualTo(child);
    }
}
