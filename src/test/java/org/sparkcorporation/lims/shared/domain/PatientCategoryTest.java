package org.sparkcorporation.lims.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PatientCategoryTest {

    @Test
    void declares_four_values_in_order() {
        assertThat(PatientCategory.values())
                .containsExactly(
                        PatientCategory.HOMME,
                        PatientCategory.FEMME,
                        PatientCategory.ENFANT,
                        PatientCategory.NOURRISSON_NOUVEAU_NE);
    }

    @Test
    void rejects_null_sex() {
        assertThatThrownBy(() -> PatientCategory.of(null, new Age(5, AgeUnit.YEARS)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_null_age() {
        assertThatThrownBy(() -> PatientCategory.of(Sex.M, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void age_below_one_year_is_NOURRISSON_NOUVEAU_NE() {
        assertThat(PatientCategory.of(Sex.M, new Age(364, AgeUnit.DAYS)))
                .isEqualTo(PatientCategory.NOURRISSON_NOUVEAU_NE);
        assertThat(PatientCategory.of(Sex.F, new Age(11, AgeUnit.MONTHS)))
                .isEqualTo(PatientCategory.NOURRISSON_NOUVEAU_NE);
        assertThat(PatientCategory.of(Sex.M, new Age(0, AgeUnit.YEARS)))
                .isEqualTo(PatientCategory.NOURRISSON_NOUVEAU_NE);
    }

    @Test
    void age_at_exact_one_year_is_ENFANT() {
        assertThat(PatientCategory.of(Sex.F, new Age(1, AgeUnit.YEARS)))
                .isEqualTo(PatientCategory.ENFANT);
        assertThat(PatientCategory.of(Sex.F, new Age(365, AgeUnit.DAYS)))
                .isEqualTo(PatientCategory.ENFANT);
    }

    @Test
    void age_between_one_and_eighteen_is_ENFANT() {
        assertThat(PatientCategory.of(Sex.M, new Age(5, AgeUnit.YEARS)))
                .isEqualTo(PatientCategory.ENFANT);
        assertThat(PatientCategory.of(Sex.F, new Age(17, AgeUnit.YEARS)))
                .isEqualTo(PatientCategory.ENFANT);
    }

    @Test
    void age_at_exactly_eighteen_years_male_is_HOMME() {
        assertThat(PatientCategory.of(Sex.M, new Age(18, AgeUnit.YEARS)))
                .isEqualTo(PatientCategory.HOMME);
    }

    @Test
    void age_at_exactly_eighteen_years_female_is_FEMME() {
        assertThat(PatientCategory.of(Sex.F, new Age(18, AgeUnit.YEARS)))
                .isEqualTo(PatientCategory.FEMME);
    }

    @Test
    void adult_male_is_HOMME() {
        assertThat(PatientCategory.of(Sex.M, new Age(40, AgeUnit.YEARS)))
                .isEqualTo(PatientCategory.HOMME);
    }

    @Test
    void adult_female_is_FEMME() {
        assertThat(PatientCategory.of(Sex.F, new Age(40, AgeUnit.YEARS)))
                .isEqualTo(PatientCategory.FEMME);
    }

    @Test
    void every_constant_name_fits_VARCHAR_25_storage() {
        // Locks the reference_intervals.category VARCHAR(25) contract from tech doc §6.3 against
        // future additions.
        for (PatientCategory c : PatientCategory.values()) {
            assertThat(c.name().length())
                    .as("PatientCategory '%s' must fit VARCHAR(25)", c.name())
                    .isLessThanOrEqualTo(25);
        }
    }
}
