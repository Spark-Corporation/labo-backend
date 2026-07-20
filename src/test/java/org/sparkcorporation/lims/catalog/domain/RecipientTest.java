package org.sparkcorporation.lims.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class RecipientTest {

    @Test
    void create_with_minimal_inputs() {
        var r = Recipient.create("TUBE_X", "Tube X", null, null, null, null);
        assertThat(r.code()).isEqualTo("TUBE_X");
        assertThat(r.color()).isNull();
        assertThat(r.volumeMl()).isNull();
        assertThat(r.defaultMatrix()).isNull();
    }

    @Test
    void create_with_all_extras() {
        var matrix = Matrix.create("SANG", "Sang", null);
        var r =
                Recipient.create(
                        "TUBE_EDTA",
                        "Tube EDTA",
                        "violet tube",
                        "violet",
                        new BigDecimal("5.00"),
                        matrix);
        assertThat(r.color()).isEqualTo("violet");
        assertThat(r.volumeMl()).isEqualTo(new BigDecimal("5.00"));
        assertThat(r.defaultMatrix()).isEqualTo(matrix);
    }

    @Test
    void create_rejects_negative_volumeMl() {
        assertThatThrownBy(
                        () ->
                                Recipient.create(
                                        "TUBE_X",
                                        "Tube X",
                                        null,
                                        null,
                                        new BigDecimal("-1.00"),
                                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }

    @Test
    void create_rejects_invalid_code() {
        assertThatThrownBy(() -> Recipient.create("tube", "X", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_rejects_blank_name() {
        assertThatThrownBy(() -> Recipient.create("TUBE_X", "  ", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_changes_only_non_null_fields() {
        var r =
                Recipient.create(
                        "TUBE_EDTA", "Tube EDTA", "old", "violet", new BigDecimal("5.00"), null);
        r.update("Tube EDTA v2", null, null, null, null);
        assertThat(r.name()).isEqualTo("Tube EDTA v2");
        assertThat(r.description()).isEqualTo("old");
        assertThat(r.color()).isEqualTo("violet");
    }

    @Test
    void update_can_change_defaultMatrix() {
        var m1 = Matrix.create("SANG", "Sang", null);
        var m2 = Matrix.create("SERUM", "Sérum", null);
        var r = Recipient.create("TUBE_EDTA", "Tube EDTA", null, null, null, m1);
        r.update(null, null, null, null, m2);
        assertThat(r.defaultMatrix()).isEqualTo(m2);
    }

    @Test
    void update_rejects_negative_volumeMl() {
        var r = Recipient.create("TUBE_X", "Tube X", null, null, null, null);
        assertThatThrownBy(() -> r.update(null, null, null, new BigDecimal("-2.00"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void code_is_immutable() {
        assertThat(Recipient.class.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName)
                .doesNotContain("setCode");
    }

    @Test
    void newly_created_is_active() {
        var r = Recipient.create("TUBE_X", "Tube X", null, null, null, null);
        assertThat(r.isActive()).isTrue();
    }
}
