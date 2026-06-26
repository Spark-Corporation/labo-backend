package org.sparkcorporation.lims.result.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary.ComponentLink;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.api.ResultState;
import org.sparkcorporation.lims.shared.domain.PatientCategory;
import org.sparkcorporation.lims.shared.domain.Sex;

class ResultTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-09T10:00:00Z"), ZoneOffset.UTC);

    private static final LabFileId LF = new LabFileId("01KQZ0WPVQA364NMRAJMKRQC84");
    private static final AnalysisRequestId AR = new AnalysisRequestId("01KQZ0WPVQA364NMRAJMKRQC85");
    private static final AnalysisId AID = new AnalysisId("01KQZ0WPVQA364NMRAJMKRQC86");

    private static NewResult simpleReal() {
        return new NewResult(
                LF,
                AR,
                AID,
                "GLU",
                AnalysisKind.SIMPLE,
                ResultType.REAL,
                List.of(),
                Sex.M,
                12000,
                PatientCategory.HOMME,
                "mg/dL",
                2,
                null,
                null,
                null);
    }

    private static NewResult simpleInteger() {
        return new NewResult(
                LF,
                AR,
                AID,
                "WBC",
                AnalysisKind.SIMPLE,
                ResultType.INTEGER,
                List.of(),
                Sex.M,
                12000,
                PatientCategory.HOMME,
                null,
                null,
                null,
                null,
                null);
    }

    @Test
    void create_with_simple_analysis_yields_one_draft_line() {
        Result r = Result.create(simpleReal(), FIXED);

        assertThat(r.getState()).isEqualTo(ResultState.DRAFT);
        assertThat(r.getLines()).hasSize(1);
        var line = r.getLines().iterator().next();
        assertThat(line.getValueInteger()).isNull();
        assertThat(line.getValueReal()).isNull();
        assertThat(line.getValueText()).isNull();
        assertThat(line.getResultType()).isEqualTo(ResultType.REAL);
        assertThat(line.getComponentCodeSnapshot()).isEqualTo("GLU");
        assertThat(line.getPosition()).isEqualTo(0);
    }

    @Test
    void create_with_grouped_analysis_yields_M_draft_lines_in_position_order() {
        var c1 = new ComponentLink(new AnalysisId("01KQZ0WPVQA364NMRAJMKRQC11"), "C1", "Comp 1", 0);
        var c2 = new ComponentLink(new AnalysisId("01KQZ0WPVQA364NMRAJMKRQC12"), "C2", "Comp 2", 1);
        var c3 = new ComponentLink(new AnalysisId("01KQZ0WPVQA364NMRAJMKRQC13"), "C3", "Comp 3", 2);

        var cmd =
                new NewResult(
                        LF,
                        AR,
                        AID,
                        "PANEL",
                        AnalysisKind.GROUPED,
                        ResultType.REAL,
                        List.of(c1, c2, c3),
                        Sex.F,
                        9000,
                        PatientCategory.FEMME,
                        null,
                        null,
                        null,
                        null,
                        null);

        Result r = Result.create(cmd, FIXED);

        assertThat(r.getState()).isEqualTo(ResultState.DRAFT);
        assertThat(r.getLines()).hasSize(3);
        assertThat(r.getLines())
                .extracting(ResultLine::getComponentCodeSnapshot)
                .containsExactly("C1", "C2", "C3");
        assertThat(r.getLines()).extracting(ResultLine::getPosition).containsExactly(0, 1, 2);
    }

    @Test
    void setLineValue_when_state_is_DRAFT_remains_DRAFT_if_other_lines_still_null() {
        var c1 = new ComponentLink(new AnalysisId("01KQZ0WPVQA364NMRAJMKRQC21"), "C1", "Comp 1", 0);
        var c2 = new ComponentLink(new AnalysisId("01KQZ0WPVQA364NMRAJMKRQC22"), "C2", "Comp 2", 1);
        var cmd =
                new NewResult(
                        LF,
                        AR,
                        AID,
                        "PANEL",
                        AnalysisKind.GROUPED,
                        ResultType.REAL,
                        List.of(c1, c2),
                        Sex.M,
                        12000,
                        PatientCategory.HOMME,
                        null,
                        null,
                        null,
                        null,
                        null);
        Result r = Result.create(cmd, FIXED);
        var firstLine = r.getLines().iterator().next();

        r.setLineValue(
                firstLine.resultLineId(),
                new NewLineValue(new NumericResult(new BigDecimal("1.23")), null),
                null);

        assertThat(r.getState()).isEqualTo(ResultState.DRAFT);
    }

    @Test
    void setLineValue_when_last_null_line_is_filled_transitions_to_ENTERED() {
        Result r = Result.create(simpleReal(), FIXED);
        var line = r.getLines().iterator().next();

        r.setLineValue(
                line.resultLineId(),
                new NewLineValue(new NumericResult(new BigDecimal("4.50")), null),
                null);

        assertThat(r.getState()).isEqualTo(ResultState.ENTERED);
    }

    @Test
    void clearLineValue_when_state_is_ENTERED_regresses_to_DRAFT() {
        Result r = Result.create(simpleReal(), FIXED);
        var line = r.getLines().iterator().next();
        r.setLineValue(
                line.resultLineId(),
                new NewLineValue(new NumericResult(new BigDecimal("4.50")), null),
                null);
        assertThat(r.getState()).isEqualTo(ResultState.ENTERED);

        r.clearLineValue(line.resultLineId());

        assertThat(r.getState()).isEqualTo(ResultState.DRAFT);
        assertThat(line.getValueReal()).isNull();
    }

    @Test
    void clearLineValue_when_state_is_DRAFT_remains_DRAFT_no_op() {
        Result r = Result.create(simpleReal(), FIXED);
        var line = r.getLines().iterator().next();

        r.clearLineValue(line.resultLineId());

        assertThat(r.getState()).isEqualTo(ResultState.DRAFT);
    }

    @Test
    void setLineValue_when_state_is_TECHNICAL_VALIDATED_throws_ResultLockedException() {
        Result r = Result.create(simpleReal(), FIXED);
        var line = r.getLines().iterator().next();
        r.setLineValue(
                line.resultLineId(),
                new NewLineValue(new NumericResult(new BigDecimal("4.50")), null),
                null);
        r.validateTechnical("01KQZ0WPVQA364NMRAJMKRQC91", FIXED);

        assertThatThrownBy(
                        () ->
                                r.setLineValue(
                                        line.resultLineId(),
                                        new NewLineValue(
                                                new NumericResult(new BigDecimal("9.99")), null),
                                        null))
                .isInstanceOf(ResultLockedException.class);
    }

    @Test
    void validateTechnical_when_state_is_DRAFT_throws_IllegalStateTransitionException() {
        Result r = Result.create(simpleReal(), FIXED);

        assertThatThrownBy(() -> r.validateTechnical("01KQZ0WPVQA364NMRAJMKRQC91", FIXED))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void validateTechnical_when_state_is_ENTERED_transitions_with_validatorBy_at() {
        Result r = Result.create(simpleReal(), FIXED);
        var line = r.getLines().iterator().next();
        r.setLineValue(
                line.resultLineId(),
                new NewLineValue(new NumericResult(new BigDecimal("4.50")), null),
                null);

        r.validateTechnical("01KQZ0WPVQA364NMRAJMKRQC91", FIXED);

        assertThat(r.getState()).isEqualTo(ResultState.TECHNICAL_VALIDATED);
        assertThat(r.getTechnicalValidatedBy()).isEqualTo("01KQZ0WPVQA364NMRAJMKRQC91");
        assertThat(r.getTechnicalValidatedAt()).isEqualTo(FIXED.instant());
    }

    @Test
    void validateBiological_when_state_is_TECHNICAL_VALIDATED_transitions() {
        Result r = Result.create(simpleReal(), FIXED);
        var line = r.getLines().iterator().next();
        r.setLineValue(
                line.resultLineId(),
                new NewLineValue(new NumericResult(new BigDecimal("4.50")), null),
                null);
        r.validateTechnical("01KQZ0WPVQA364NMRAJMKRQC91", FIXED);

        r.validateBiological("01KQZ0WPVQA364NMRAJMKRQC92", FIXED);

        assertThat(r.getState()).isEqualTo(ResultState.BIOLOGICAL_VALIDATED);
        assertThat(r.getBiologicalValidatedBy()).isEqualTo("01KQZ0WPVQA364NMRAJMKRQC92");
        assertThat(r.getBiologicalValidatedAt()).isEqualTo(FIXED.instant());
    }

    @Test
    void validateBiological_when_state_is_ENTERED_throws() {
        Result r = Result.create(simpleReal(), FIXED);
        var line = r.getLines().iterator().next();
        r.setLineValue(
                line.resultLineId(),
                new NewLineValue(new NumericResult(new BigDecimal("4.50")), null),
                null);

        assertThatThrownBy(() -> r.validateBiological("01KQZ0WPVQA364NMRAJMKRQC92", FIXED))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void delete_when_state_is_DRAFT_passes() {
        Result r = Result.create(simpleInteger(), FIXED);

        r.delete(); // no exception
    }

    @Test
    void delete_when_state_is_ENTERED_throws_ResultNotDeletableInStateException() {
        Result r = Result.create(simpleReal(), FIXED);
        var line = r.getLines().iterator().next();
        r.setLineValue(
                line.resultLineId(),
                new NewLineValue(new NumericResult(new BigDecimal("4.50")), null),
                null);

        assertThatThrownBy(r::delete).isInstanceOf(ResultNotDeletableInStateException.class);
    }
}
