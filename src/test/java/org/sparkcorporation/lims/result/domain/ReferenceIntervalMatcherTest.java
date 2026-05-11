package org.sparkcorporation.lims.result.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.ReferenceIntervalId;
import org.sparkcorporation.lims.catalog.api.ReferenceIntervalSummary;
import org.sparkcorporation.lims.catalog.api.ReferenceMode;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.shared.domain.AgeUnit;
import org.sparkcorporation.lims.shared.domain.PatientCategory;
import org.sparkcorporation.lims.shared.domain.Sex;

class ReferenceIntervalMatcherTest {

    private static final AnalysisId AID = new AnalysisId("01KQZ0WPVQA364NMRAJMKRQC86");

    private static ReferenceIntervalSummary ri(
            String id,
            PatientCategory category,
            @Nullable Sex sex,
            @Nullable Integer ageMin,
            @Nullable Integer ageMax,
            @Nullable AgeUnit unit,
            ReferenceMode mode,
            @Nullable BigDecimal min,
            @Nullable BigDecimal max) {
        return new ReferenceIntervalSummary(
                new ReferenceIntervalId(id),
                AID,
                category,
                sex,
                ageMin,
                ageMax,
                unit,
                mode,
                min,
                max,
                null,
                null,
                mode == ReferenceMode.COMMENT ? "comment" : null);
    }

    @Test
    void match_picks_sex_specific_over_sex_null() {
        var nullSex =
                ri(
                        "01KQZ0WPVQA364NMRAJMKRQC01",
                        PatientCategory.HOMME,
                        null,
                        18,
                        100,
                        AgeUnit.YEARS,
                        ReferenceMode.STANDARD,
                        new BigDecimal("0.5"),
                        new BigDecimal("2.0"));
        var maleSpecific =
                ri(
                        "01KQZ0WPVQA364NMRAJMKRQC02",
                        PatientCategory.HOMME,
                        Sex.M,
                        18,
                        100,
                        AgeUnit.YEARS,
                        ReferenceMode.STANDARD,
                        new BigDecimal("0.6"),
                        new BigDecimal("1.8"));

        var winner =
                ReferenceIntervalMatcher.match(
                        PatientCategory.HOMME, Sex.M, 30 * 365, List.of(nullSex, maleSpecific));

        assertThat(winner).isPresent();
        assertThat(winner.get().id().value()).isEqualTo("01KQZ0WPVQA364NMRAJMKRQC02");
    }

    @Test
    void match_picks_narrower_age_window_when_both_match() {
        var wide =
                ri(
                        "01KQZ0WPVQA364NMRAJMKRQC01",
                        PatientCategory.HOMME,
                        Sex.M,
                        0,
                        100,
                        AgeUnit.YEARS,
                        ReferenceMode.STANDARD,
                        new BigDecimal("0.5"),
                        new BigDecimal("2.0"));
        var narrow =
                ri(
                        "01KQZ0WPVQA364NMRAJMKRQC02",
                        PatientCategory.HOMME,
                        Sex.M,
                        25,
                        40,
                        AgeUnit.YEARS,
                        ReferenceMode.STANDARD,
                        new BigDecimal("0.6"),
                        new BigDecimal("1.8"));

        var winner =
                ReferenceIntervalMatcher.match(
                        PatientCategory.HOMME, Sex.M, 30 * 365, List.of(wide, narrow));

        assertThat(winner).isPresent();
        assertThat(winner.get().id().value()).isEqualTo("01KQZ0WPVQA364NMRAJMKRQC02");
    }

    @Test
    void match_returns_empty_when_no_category_matches() {
        var only =
                ri(
                        "01KQZ0WPVQA364NMRAJMKRQC01",
                        PatientCategory.FEMME,
                        Sex.F,
                        18,
                        100,
                        AgeUnit.YEARS,
                        ReferenceMode.STANDARD,
                        new BigDecimal("0.5"),
                        new BigDecimal("2.0"));

        var winner =
                ReferenceIntervalMatcher.match(
                        PatientCategory.HOMME, Sex.M, 30 * 365, List.of(only));

        assertThat(winner).isEmpty();
    }

    @Test
    void match_returns_empty_when_age_outside_window() {
        var ri =
                ri(
                        "01KQZ0WPVQA364NMRAJMKRQC01",
                        PatientCategory.HOMME,
                        Sex.M,
                        18,
                        50,
                        AgeUnit.YEARS,
                        ReferenceMode.STANDARD,
                        new BigDecimal("0.5"),
                        new BigDecimal("2.0"));

        var winner =
                ReferenceIntervalMatcher.match(PatientCategory.HOMME, Sex.M, 70 * 365, List.of(ri));

        assertThat(winner).isEmpty();
    }

    @Test
    void outOfRange_STANDARD_below_unit_1_min_returns_true() {
        var standard =
                ri(
                        "01KQZ0WPVQA364NMRAJMKRQC01",
                        PatientCategory.HOMME,
                        Sex.M,
                        18,
                        100,
                        AgeUnit.YEARS,
                        ReferenceMode.STANDARD,
                        new BigDecimal("4.0"),
                        new BigDecimal("8.0"));

        boolean out =
                ReferenceIntervalMatcher.isOutOfRange(
                        ResultType.REAL, null, new BigDecimal("3.5"), null, standard);

        assertThat(out).isTrue();
    }

    @Test
    void outOfRange_STANDARD_above_unit_1_max_returns_true() {
        var standard =
                ri(
                        "01KQZ0WPVQA364NMRAJMKRQC01",
                        PatientCategory.HOMME,
                        Sex.M,
                        18,
                        100,
                        AgeUnit.YEARS,
                        ReferenceMode.STANDARD,
                        new BigDecimal("4.0"),
                        new BigDecimal("8.0"));

        boolean out =
                ReferenceIntervalMatcher.isOutOfRange(
                        ResultType.REAL, null, new BigDecimal("9.5"), null, standard);

        assertThat(out).isTrue();
    }

    @Test
    void outOfRange_STANDARD_inside_window_returns_false() {
        var standard =
                ri(
                        "01KQZ0WPVQA364NMRAJMKRQC01",
                        PatientCategory.HOMME,
                        Sex.M,
                        18,
                        100,
                        AgeUnit.YEARS,
                        ReferenceMode.STANDARD,
                        new BigDecimal("4.0"),
                        new BigDecimal("8.0"));

        boolean out =
                ReferenceIntervalMatcher.isOutOfRange(
                        ResultType.REAL, null, new BigDecimal("6.0"), null, standard);

        assertThat(out).isFalse();
    }

    @Test
    void outOfRange_COMMENT_mode_always_returns_false() {
        var comment =
                ri(
                        "01KQZ0WPVQA364NMRAJMKRQC01",
                        PatientCategory.HOMME,
                        Sex.M,
                        18,
                        100,
                        AgeUnit.YEARS,
                        ReferenceMode.COMMENT,
                        null,
                        null);

        boolean out =
                ReferenceIntervalMatcher.isOutOfRange(
                        ResultType.REAL, null, new BigDecimal("9999.0"), null, comment);

        assertThat(out).isFalse();
    }

    @Test
    void outOfRange_NONE_mode_always_returns_false() {
        var none =
                ri(
                        "01KQZ0WPVQA364NMRAJMKRQC01",
                        PatientCategory.HOMME,
                        Sex.M,
                        18,
                        100,
                        AgeUnit.YEARS,
                        ReferenceMode.NONE,
                        null,
                        null);

        boolean out =
                ReferenceIntervalMatcher.isOutOfRange(
                        ResultType.REAL, null, new BigDecimal("9999.0"), null, none);

        assertThat(out).isFalse();
    }

    @Test
    void outOfRange_TEXT_value_skips_scoring_returns_false() {
        var standard =
                ri(
                        "01KQZ0WPVQA364NMRAJMKRQC01",
                        PatientCategory.HOMME,
                        Sex.M,
                        18,
                        100,
                        AgeUnit.YEARS,
                        ReferenceMode.STANDARD,
                        new BigDecimal("4.0"),
                        new BigDecimal("8.0"));

        boolean out =
                ReferenceIntervalMatcher.isOutOfRange(
                        ResultType.TEXT, null, null, "Positif", standard);

        assertThat(out).isFalse();
    }

    @Test
    void match_with_no_referenceIntervals_returns_no_match() {
        var winner =
                ReferenceIntervalMatcher.match(PatientCategory.HOMME, Sex.M, 30 * 365, List.of());

        assertThat(winner).isEmpty();
    }
}
