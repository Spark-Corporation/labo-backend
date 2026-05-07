package org.sparkcorporation.lims.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.ReferenceMode;
import org.sparkcorporation.lims.shared.domain.AgeUnit;
import org.sparkcorporation.lims.shared.domain.PatientCategory;
import org.sparkcorporation.lims.shared.domain.Sex;

class ReferenceIntervalTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-07T10:00:00Z"), ZoneOffset.UTC);
    private static final AnalysisId ANALYSIS_ID = AnalysisId.generate();

    @Test
    void standard_mode_requires_unit1_min_and_max() {
        var cmd =
                new NewReferenceInterval(
                        PatientCategory.HOMME,
                        Sex.M,
                        18,
                        99,
                        AgeUnit.YEARS,
                        ReferenceMode.STANDARD,
                        null,
                        new BigDecimal("110.0"),
                        null,
                        null,
                        null);
        assertThatThrownBy(() -> ReferenceInterval.create(cmd, ANALYSIS_ID, FIXED))
                .isInstanceOf(InvalidReferenceIntervalException.class)
                .hasMessageContaining("STANDARD");
    }

    @Test
    void standard_mode_rejects_unit1_min_greater_than_max() {
        var cmd =
                new NewReferenceInterval(
                        PatientCategory.HOMME,
                        Sex.M,
                        18,
                        99,
                        AgeUnit.YEARS,
                        ReferenceMode.STANDARD,
                        new BigDecimal("110.0"),
                        new BigDecimal("70.0"),
                        null,
                        null,
                        null);
        assertThatThrownBy(() -> ReferenceInterval.create(cmd, ANALYSIS_ID, FIXED))
                .isInstanceOf(InvalidReferenceIntervalException.class)
                .hasMessageContaining("unit1Min");
    }

    @Test
    void standard_mode_rejects_when_comment_text_present() {
        var cmd =
                new NewReferenceInterval(
                        PatientCategory.HOMME,
                        Sex.M,
                        18,
                        99,
                        AgeUnit.YEARS,
                        ReferenceMode.STANDARD,
                        new BigDecimal("70.0"),
                        new BigDecimal("110.0"),
                        null,
                        null,
                        "must not be set");
        assertThatThrownBy(() -> ReferenceInterval.create(cmd, ANALYSIS_ID, FIXED))
                .isInstanceOf(InvalidReferenceIntervalException.class)
                .hasMessageContaining("commentText");
    }

    @Test
    void comment_mode_requires_comment_text() {
        var cmd =
                new NewReferenceInterval(
                        PatientCategory.HOMME,
                        Sex.M,
                        18,
                        99,
                        AgeUnit.YEARS,
                        ReferenceMode.COMMENT,
                        null,
                        null,
                        null,
                        null,
                        null);
        assertThatThrownBy(() -> ReferenceInterval.create(cmd, ANALYSIS_ID, FIXED))
                .isInstanceOf(InvalidReferenceIntervalException.class)
                .hasMessageContaining("COMMENT");
    }

    @Test
    void comment_mode_rejects_blank_comment_text() {
        var cmd =
                new NewReferenceInterval(
                        PatientCategory.HOMME,
                        Sex.M,
                        18,
                        99,
                        AgeUnit.YEARS,
                        ReferenceMode.COMMENT,
                        null,
                        null,
                        null,
                        null,
                        "   ");
        assertThatThrownBy(() -> ReferenceInterval.create(cmd, ANALYSIS_ID, FIXED))
                .isInstanceOf(InvalidReferenceIntervalException.class)
                .hasMessageContaining("COMMENT");
    }

    @Test
    void comment_mode_rejects_numeric_fields() {
        var cmd =
                new NewReferenceInterval(
                        PatientCategory.HOMME,
                        Sex.M,
                        18,
                        99,
                        AgeUnit.YEARS,
                        ReferenceMode.COMMENT,
                        new BigDecimal("70.0"),
                        null,
                        null,
                        null,
                        "see notes");
        assertThatThrownBy(() -> ReferenceInterval.create(cmd, ANALYSIS_ID, FIXED))
                .isInstanceOf(InvalidReferenceIntervalException.class)
                .hasMessageContaining("numeric");
    }

    @Test
    void none_mode_rejects_all_data_fields() {
        var cmd =
                new NewReferenceInterval(
                        PatientCategory.HOMME,
                        Sex.M,
                        18,
                        99,
                        AgeUnit.YEARS,
                        ReferenceMode.NONE,
                        null,
                        null,
                        null,
                        null,
                        "anything");
        assertThatThrownBy(() -> ReferenceInterval.create(cmd, ANALYSIS_ID, FIXED))
                .isInstanceOf(InvalidReferenceIntervalException.class)
                .hasMessageContaining("NONE");
    }

    @Test
    void clone_for_category_preserves_data_with_new_category_and_fresh_id() {
        var ri = standardInterval();
        var clone = ri.cloneForCategory(PatientCategory.FEMME, FIXED);
        assertThat(clone.id().value()).isNotEqualTo(ri.id().value());
        assertThat(clone.category()).isEqualTo(PatientCategory.FEMME);
        assertThat(clone.rawAnalysisId()).isEqualTo(ri.rawAnalysisId());
        assertThat(clone.unit1Min()).isEqualTo(ri.unit1Min());
        assertThat(clone.unit1Max()).isEqualTo(ri.unit1Max());
        assertThat(clone.mode()).isEqualTo(ri.mode());
    }

    @Test
    void clone_for_analysis_preserves_data_with_new_analysis_id_and_fresh_id() {
        var ri = standardInterval();
        var newAnalysisId = AnalysisId.generate();
        var clone = ri.cloneForAnalysis(newAnalysisId, FIXED);
        assertThat(clone.id().value()).isNotEqualTo(ri.id().value());
        assertThat(clone.rawAnalysisId()).isEqualTo(newAnalysisId.value());
        assertThat(clone.category()).isEqualTo(ri.category());
        assertThat(clone.unit1Min()).isEqualTo(ri.unit1Min());
        assertThat(clone.unit1Max()).isEqualTo(ri.unit1Max());
    }

    @Test
    void is_active_true_when_not_deleted() {
        var ri = standardInterval();
        assertThat(ri.isActive()).isTrue();
        assertThat(ri.deletedAt()).isNull();
    }

    private static ReferenceInterval standardInterval() {
        var cmd =
                new NewReferenceInterval(
                        PatientCategory.HOMME,
                        Sex.M,
                        18,
                        99,
                        AgeUnit.YEARS,
                        ReferenceMode.STANDARD,
                        new BigDecimal("70.0"),
                        new BigDecimal("110.0"),
                        null,
                        null,
                        null);
        return ReferenceInterval.create(cmd, ANALYSIS_ID, FIXED);
    }
}
