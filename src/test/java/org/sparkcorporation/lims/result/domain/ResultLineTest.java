package org.sparkcorporation.lims.result.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.shared.domain.PatientCategory;
import org.sparkcorporation.lims.shared.domain.Sex;

class ResultLineTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-09T10:00:00Z"), ZoneOffset.UTC);

    private static Result newResultWithSingleLine(ResultType type) {
        var cmd =
                new NewResult(
                        new LabFileId("01KQZ0WPVQA364NMRAJMKRQC84"),
                        new AnalysisRequestId("01KQZ0WPVQA364NMRAJMKRQC85"),
                        new AnalysisId("01KQZ0WPVQA364NMRAJMKRQC86"),
                        "TEST",
                        AnalysisKind.SIMPLE,
                        type,
                        List.of(),
                        Sex.M,
                        12000,
                        PatientCategory.HOMME,
                        null,
                        null,
                        null,
                        null);
        return Result.create(cmd, FIXED);
    }

    @Test
    void kindMismatch_INTEGER_line_with_REAL_value_throws_ResultLineKindMismatchException() {
        Result r = newResultWithSingleLine(ResultType.INTEGER);
        var line = r.getLines().iterator().next();
        var lineId = line.resultLineId();

        assertThatThrownBy(
                        () ->
                                r.setLineValue(
                                        lineId,
                                        new NewLineValue(
                                                new NumericResult(new BigDecimal("4.50")), null),
                                        null))
                .isInstanceOf(ResultLineKindMismatchException.class)
                .hasMessageContaining("INTEGER")
                .hasMessageContaining("NumericResult")
                .satisfies(
                        ex ->
                                assertThat(((ResultLineKindMismatchException) ex).lineId())
                                        .isEqualTo(lineId))
                .satisfies(
                        ex ->
                                assertThat(((ResultLineKindMismatchException) ex).lineType())
                                        .isEqualTo(ResultType.INTEGER))
                .satisfies(
                        ex ->
                                assertThat(((ResultLineKindMismatchException) ex).requestKind())
                                        .isEqualTo("NumericResult"));
    }

    @Test
    void setValue_then_clearValue_resets_all_three_columns_and_isOutOfRange() {
        Result r = newResultWithSingleLine(ResultType.REAL);
        var line = r.getLines().iterator().next();
        r.setLineValue(
                line.resultLineId(),
                new NewLineValue(new NumericResult(new BigDecimal("4.50")), null),
                null);
        assertThat(line.hasValue()).isTrue();

        r.clearLineValue(line.resultLineId());

        assertThat(line.getValueInteger()).isNull();
        assertThat(line.getValueReal()).isNull();
        assertThat(line.getValueText()).isNull();
        assertThat(line.isOutOfRange()).isFalse();
        assertThat(line.hasValue()).isFalse();
    }

    /**
     * Reflectively forces a malformed row state — {@code resultType=REAL} with only {@code
     * valueText} populated — and invokes the package-private {@code @PostLoad} hook. The defensive
     * check in {@code rehydrateValue} should throw.
     */
    @Test
    void postLoad_with_inconsistent_resultType_and_value_columns_throws() throws Exception {
        Result r = newResultWithSingleLine(ResultType.REAL);
        var line = r.getLines().iterator().next();

        // Force the malformed state via reflection — bypass the aggregate's setValue guards.
        Field valueText = ResultLine.class.getDeclaredField("valueText");
        valueText.setAccessible(true);
        valueText.set(line, "should not be here");

        // @PostLoad is package-private; call directly.
        java.lang.reflect.Method m = ResultLine.class.getDeclaredMethod("rehydrateValue");
        m.setAccessible(true);
        assertThatThrownBy(() -> m.invoke(line)).hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void setValue_INTEGER_persists_to_valueInteger_column() {
        Result r = newResultWithSingleLine(ResultType.INTEGER);
        var line = r.getLines().iterator().next();

        r.setLineValue(line.resultLineId(), new NewLineValue(new IntegerResult(42L), null), null);

        assertThat(line.getValueInteger()).isEqualTo(42L);
        assertThat(line.getValueReal()).isNull();
        assertThat(line.getValueText()).isNull();
        assertThat(line.hasValue()).isTrue();
    }

    @Test
    void setValue_REAL_persists_to_valueReal_column() {
        Result r = newResultWithSingleLine(ResultType.REAL);
        var line = r.getLines().iterator().next();

        r.setLineValue(
                line.resultLineId(),
                new NewLineValue(new NumericResult(new BigDecimal("4.20")), null),
                null);

        assertThat(line.getValueInteger()).isNull();
        assertThat(line.getValueReal()).isEqualByComparingTo("4.20");
        assertThat(line.getValueText()).isNull();
    }

    @Test
    void setValue_TEXT_persists_to_valueText_column() {
        Result r = newResultWithSingleLine(ResultType.TEXT);
        var line = r.getLines().iterator().next();

        r.setLineValue(
                line.resultLineId(), new NewLineValue(new TextResult("Positif"), null), null);

        assertThat(line.getValueInteger()).isNull();
        assertThat(line.getValueReal()).isNull();
        assertThat(line.getValueText()).isEqualTo("Positif");
    }

    @Test
    void setValue_FREE_persists_to_valueText_column() {
        Result r = newResultWithSingleLine(ResultType.FREE);
        var line = r.getLines().iterator().next();

        r.setLineValue(
                line.resultLineId(),
                new NewLineValue(new FreeFormResult("indéterminé"), null),
                null);

        assertThat(line.getValueText()).isEqualTo("indéterminé");
    }

    @Test
    void setValue_with_null_value_clears_all_value_columns() {
        Result r = newResultWithSingleLine(ResultType.REAL);
        var line = r.getLines().iterator().next();
        r.setLineValue(
                line.resultLineId(),
                new NewLineValue(new NumericResult(new BigDecimal("4.50")), null),
                null);
        assertThat(line.hasValue()).isTrue();

        // explicit null value via the cmd → should clear
        r.setLineValue(line.resultLineId(), new NewLineValue(null, null), null);

        assertThat(line.hasValue()).isFalse();
        assertThat(line.isOutOfRange()).isFalse();
        assertThat(line.getReferenceIntervalId()).isNull();
    }

    @Test
    void setValue_with_empty_comment_normalizes_to_null() {
        Result r = newResultWithSingleLine(ResultType.REAL);
        var line = r.getLines().iterator().next();

        r.setLineValue(
                line.resultLineId(),
                new NewLineValue(new NumericResult(new BigDecimal("4.20")), ""),
                null);

        assertThat(line.getCommentText()).isNull();
    }

    @Test
    void setValue_with_non_empty_comment_persists_it() {
        Result r = newResultWithSingleLine(ResultType.REAL);
        var line = r.getLines().iterator().next();

        r.setLineValue(
                line.resultLineId(),
                new NewLineValue(new NumericResult(new BigDecimal("4.20")), "à vérifier"),
                null);

        assertThat(line.getCommentText()).isEqualTo("à vérifier");
    }

    @Test
    void kindMismatch_TEXT_line_with_INTEGER_value_throws() {
        Result r = newResultWithSingleLine(ResultType.TEXT);
        var line = r.getLines().iterator().next();

        assertThatThrownBy(
                        () ->
                                r.setLineValue(
                                        line.resultLineId(),
                                        new NewLineValue(new IntegerResult(1L), null),
                                        null))
                .isInstanceOf(ResultLineKindMismatchException.class)
                .hasMessageContaining("TEXT");
    }

    @Test
    void kindMismatch_REAL_line_with_TEXT_value_throws() {
        Result r = newResultWithSingleLine(ResultType.REAL);
        var line = r.getLines().iterator().next();

        assertThatThrownBy(
                        () ->
                                r.setLineValue(
                                        line.resultLineId(),
                                        new NewLineValue(new TextResult("nope"), null),
                                        null))
                .isInstanceOf(ResultLineKindMismatchException.class);
    }

    @Test
    void kindMismatch_FORMULA_line_with_REAL_value_throws() {
        Result r = newResultWithSingleLine(ResultType.FORMULA);
        var line = r.getLines().iterator().next();

        assertThatThrownBy(
                        () ->
                                r.setLineValue(
                                        line.resultLineId(),
                                        new NewLineValue(
                                                new NumericResult(new BigDecimal("1")), null),
                                        null))
                .isInstanceOf(ResultLineKindMismatchException.class);
    }

    @Test
    void kindMismatch_FREE_line_with_INTEGER_value_throws() {
        Result r = newResultWithSingleLine(ResultType.FREE);
        var line = r.getLines().iterator().next();

        assertThatThrownBy(
                        () ->
                                r.setLineValue(
                                        line.resultLineId(),
                                        new NewLineValue(new IntegerResult(1L), null),
                                        null))
                .isInstanceOf(ResultLineKindMismatchException.class);
    }
}
