package org.sparkcorporation.lims.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DisciplineTest {

    @Test
    void create_with_valid_inputs_returns_active_aggregate() {
        var d = Discipline.create("BIOCHIMIE", "Biochimie", null);
        assertThat(d.code()).isEqualTo("BIOCHIMIE");
        assertThat(d.name()).isEqualTo("Biochimie");
        assertThat(d.isActive()).isTrue();
        assertThat(d.deletedAt()).isNull();
    }

    @Test
    void create_rejects_invalid_code_format() {
        assertThatThrownBy(() -> Discipline.create("biochimie", "Biochimie", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid code");
    }

    @Test
    void create_rejects_blank_name() {
        assertThatThrownBy(() -> Discipline.create("BIOCHIMIE", "  ", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_trims_name() {
        assertThat(Discipline.create("BIO", "  Biochimie  ", null).name()).isEqualTo("Biochimie");
    }

    @Test
    void update_changes_name_when_non_null() {
        var d = Discipline.create("BIO", "Biochimie", null);
        d.update("Biochimie clinique", null);
        assertThat(d.name()).isEqualTo("Biochimie clinique");
    }

    @Test
    void update_skips_null_fields() {
        var d = Discipline.create("BIO", "Biochimie", "desc");
        d.update(null, null);
        assertThat(d.name()).isEqualTo("Biochimie");
        assertThat(d.description()).isEqualTo("desc");
    }

    @Test
    void update_rejects_blank_name() {
        var d = Discipline.create("BIO", "Biochimie", null);
        assertThatThrownBy(() -> d.update("  ", null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void code_is_intentionally_immutable_no_setter() {
        assertThat(Discipline.class.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName)
                .doesNotContain("setCode");
    }
}
