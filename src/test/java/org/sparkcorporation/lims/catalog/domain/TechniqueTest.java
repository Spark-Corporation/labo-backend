package org.sparkcorporation.lims.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TechniqueTest {

    @Test
    void create_with_valid_inputs_returns_active_aggregate() {
        var t = Technique.create("ELISA", "ELISA", null);
        assertThat(t.code()).isEqualTo("ELISA");
        assertThat(t.name()).isEqualTo("ELISA");
        assertThat(t.isActive()).isTrue();
        assertThat(t.deletedAt()).isNull();
    }

    @Test
    void create_rejects_invalid_code_format() {
        assertThatThrownBy(() -> Technique.create("elisa", "ELISA", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid code");
    }

    @Test
    void create_rejects_blank_name() {
        assertThatThrownBy(() -> Technique.create("ELISA", "  ", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_trims_name() {
        assertThat(Technique.create("ELISA", "  ELISA  ", null).name()).isEqualTo("ELISA");
    }

    @Test
    void update_changes_name_when_non_null() {
        var t = Technique.create("ELISA", "ELISA", null);
        t.update("ELISA v2", null);
        assertThat(t.name()).isEqualTo("ELISA v2");
    }

    @Test
    void update_skips_null_fields() {
        var t = Technique.create("ELISA", "ELISA", "desc");
        t.update(null, null);
        assertThat(t.name()).isEqualTo("ELISA");
        assertThat(t.description()).isEqualTo("desc");
    }

    @Test
    void update_rejects_blank_name() {
        var t = Technique.create("ELISA", "ELISA", null);
        assertThatThrownBy(() -> t.update("  ", null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void code_is_intentionally_immutable_no_setter() {
        assertThat(Technique.class.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName)
                .doesNotContain("setCode");
    }
}
