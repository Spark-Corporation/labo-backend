package org.sparkcorporation.lims.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class AntibioticTest {

    @Test
    void create_with_minimal_inputs() {
        var a =
                Antibiotic.create(
                        "AMX", "Amoxicilline", null, null, new BigDecimal("21.00"), null, null);
        assertThat(a.code()).isEqualTo("AMX");
        assertThat(a.referenceDiameter()).isEqualByComparingTo("21.00");
    }

    @Test
    void create_with_all_extras() {
        var a =
                Antibiotic.create(
                        "AMX",
                        "Amoxicilline",
                        "broad spectrum",
                        "Beta-lactamines",
                        new BigDecimal("21.00"),
                        new BigDecimal("16.00"),
                        new BigDecimal("21.00"));
        assertThat(a.family()).isEqualTo("Beta-lactamines");
        assertThat(a.intermediateLower()).isEqualByComparingTo("16.00");
        assertThat(a.intermediateUpper()).isEqualByComparingTo("21.00");
    }

    @Test
    void create_rejects_null_referenceDiameter() {
        assertThatThrownBy(
                        () ->
                                Antibiotic.create(
                                        "AMX", "Amoxicilline", null, null, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejects_negative_referenceDiameter() {
        assertThatThrownBy(
                        () ->
                                Antibiotic.create(
                                        "AMX",
                                        "Amoxicilline",
                                        null,
                                        null,
                                        new BigDecimal("-1.00"),
                                        null,
                                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }

    @Test
    void create_accepts_zero_referenceDiameter_for_seed_placeholder() {
        // Per spec §4.5: seeded antibiotics have reference_diameter = 0.00 placeholder
        var a = Antibiotic.create("AMX", "Amoxicilline", null, null, BigDecimal.ZERO, null, null);
        assertThat(a.referenceDiameter()).isEqualByComparingTo("0.00");
    }

    /** Spec §10.1 first-failing unit test (most domain-sensitive validation in 1.3.a). */
    @Test
    void create_rejects_intermediate_lower_greater_than_upper() {
        assertThatThrownBy(
                        () ->
                                Antibiotic.create(
                                        "AMX",
                                        "Amoxicilline",
                                        null,
                                        null,
                                        new BigDecimal("21.00"),
                                        new BigDecimal("22.00"), // lower > upper
                                        new BigDecimal("18.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("intermediateLower must be ≤ intermediateUpper");
    }

    @Test
    void create_accepts_equal_intermediate_lower_and_upper() {
        var a =
                Antibiotic.create(
                        "AMX",
                        "Amoxicilline",
                        null,
                        null,
                        new BigDecimal("21.00"),
                        new BigDecimal("18.00"),
                        new BigDecimal("18.00"));
        assertThat(a.intermediateLower()).isEqualByComparingTo("18.00");
    }

    @Test
    void create_accepts_only_lower_or_only_upper() {
        var lowerOnly =
                Antibiotic.create(
                        "AMX",
                        "X",
                        null,
                        null,
                        new BigDecimal("21.00"),
                        new BigDecimal("16.00"),
                        null);
        var upperOnly =
                Antibiotic.create(
                        "AMX2",
                        "Y",
                        null,
                        null,
                        new BigDecimal("21.00"),
                        null,
                        new BigDecimal("21.00"));
        assertThat(lowerOnly.intermediateLower()).isNotNull();
        assertThat(upperOnly.intermediateUpper()).isNotNull();
    }

    @Test
    void update_changes_only_non_null_fields() {
        var a =
                Antibiotic.create(
                        "AMX", "Amoxicilline", null, null, new BigDecimal("21.00"), null, null);
        a.update("Amoxi v2", null, null, new BigDecimal("22.00"), null, null);
        assertThat(a.name()).isEqualTo("Amoxi v2");
        assertThat(a.referenceDiameter()).isEqualByComparingTo("22.00");
    }

    @Test
    void update_re_validates_intermediate_pair() {
        var a =
                Antibiotic.create(
                        "AMX",
                        "Amoxicilline",
                        null,
                        null,
                        new BigDecimal("21.00"),
                        new BigDecimal("16.00"),
                        new BigDecimal("21.00"));
        assertThatThrownBy(
                        () ->
                                a.update(
                                        null,
                                        null,
                                        null,
                                        null,
                                        new BigDecimal("99.00"),
                                        new BigDecimal("18.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
