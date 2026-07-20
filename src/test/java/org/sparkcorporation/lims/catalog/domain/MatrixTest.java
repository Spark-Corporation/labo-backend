package org.sparkcorporation.lims.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MatrixTest {

    @Test
    void create_with_valid_inputs_returns_active_aggregate() {
        var m = Matrix.create("SANG", "Sang", null);
        assertThat(m.code()).isEqualTo("SANG");
        assertThat(m.name()).isEqualTo("Sang");
        assertThat(m.isActive()).isTrue();
        assertThat(m.deletedAt()).isNull();
    }

    @Test
    void create_rejects_invalid_code_format() {
        assertThatThrownBy(() -> Matrix.create("sang", "Sang", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid code");
    }

    @Test
    void create_rejects_blank_name() {
        assertThatThrownBy(() -> Matrix.create("SANG", "  ", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_trims_name() {
        assertThat(Matrix.create("SANG", "  Sang  ", null).name()).isEqualTo("Sang");
    }

    @Test
    void update_changes_name_when_non_null() {
        var m = Matrix.create("SANG", "Sang", null);
        m.update("Sang total", null);
        assertThat(m.name()).isEqualTo("Sang total");
    }

    @Test
    void update_skips_null_fields() {
        var m = Matrix.create("SANG", "Sang", "desc");
        m.update(null, null);
        assertThat(m.name()).isEqualTo("Sang");
        assertThat(m.description()).isEqualTo("desc");
    }

    @Test
    void update_rejects_blank_name() {
        var m = Matrix.create("SANG", "Sang", null);
        assertThatThrownBy(() -> m.update("  ", null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void code_is_intentionally_immutable_no_setter() {
        assertThat(Matrix.class.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName)
                .doesNotContain("setCode");
    }
}
