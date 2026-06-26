package org.sparkcorporation.lims.result.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.domain.NewLineValue;
import org.sparkcorporation.lims.result.domain.NewResult;
import org.sparkcorporation.lims.result.domain.NumericResult;
import org.sparkcorporation.lims.result.domain.Result;
import org.sparkcorporation.lims.result.infra.ResultRepository;
import org.sparkcorporation.lims.shared.domain.PatientCategory;
import org.sparkcorporation.lims.shared.domain.Sex;

class FormulaRecalculationServiceTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-06-24T10:00:00Z"), ZoneOffset.UTC);
    private static final String LF = "01KQZ0WPVQA364NMRAJMKRQC84";

    private final ResultRepository repo = mock(ResultRepository.class);
    private final AnalysisApi analysisApi = mock(AnalysisApi.class);
    private final FormulaRecalculationService service =
            new FormulaRecalculationService(repo, analysisApi);

    // --- helpers -------------------------------------------------------------------------------

    private static final String[] ULIDS = {
        "01KQZ0WPVQA364NMRAJMKRQC01",
        "01KQZ0WPVQA364NMRAJMKRQC02",
        "01KQZ0WPVQA364NMRAJMKRQC03",
        "01KQZ0WPVQA364NMRAJMKRQC04",
        "01KQZ0WPVQA364NMRAJMKRQC05",
        "01KQZ0WPVQA364NMRAJMKRQC06"
    };

    private int idx;

    private String nextId() {
        return ULIDS[idx++];
    }

    private Result result(String code, String analysisId, ResultType type) {
        var cmd =
                new NewResult(
                        new LabFileId(LF),
                        new AnalysisRequestId(nextId()),
                        new AnalysisId(analysisId),
                        code,
                        AnalysisKind.SIMPLE,
                        type,
                        List.of(),
                        Sex.M,
                        12000,
                        PatientCategory.HOMME,
                        null,
                        null,
                        null,
                        null,
                        null);
        return Result.create(cmd, FIXED);
    }

    private Result measured(String code, String analysisId, String value) {
        Result r = result(code, analysisId, ResultType.REAL);
        var line = r.getLines().iterator().next();
        r.setLineValue(
                line.resultLineId(),
                new NewLineValue(new NumericResult(new BigDecimal(value)), null),
                null);
        return r;
    }

    private Result formula(String code, String analysisId, String expression) {
        Result r = result(code, analysisId, ResultType.FORMULA);
        var a = mock(AnalysisSummary.class);
        lenient().when(a.formulaExpression()).thenReturn(expression);
        when(analysisApi.findById(new AnalysisId(analysisId))).thenReturn(Optional.of(a));
        lenient()
                .when(
                        analysisApi.findActiveReferenceIntervalsByAnalysis(
                                new AnalysisId(analysisId)))
                .thenReturn(List.of());
        return r;
    }

    private static BigDecimal valueOf(Result r) {
        return r.getLines().iterator().next().getValueReal();
    }

    // --- tests ---------------------------------------------------------------------------------

    @Test
    void computes_ldl_from_measured_siblings_across_dossier() {
        var ct = measured("CT", "01KQZ0WPVQA364NMRAJMKRQCA1", "2.00");
        var hdl = measured("HDL", "01KQZ0WPVQA364NMRAJMKRQCA2", "0.50");
        var tg = measured("TG", "01KQZ0WPVQA364NMRAJMKRQCA3", "1.00");
        var ldl = formula("LDL", "01KQZ0WPVQA364NMRAJMKRQCA4", "CT - HDL - TG/5");
        when(repo.findActiveByLabFileId(LF)).thenReturn(List.of(ct, hdl, tg, ldl));

        service.recompute(LF);

        // 2.00 - 0.50 - 0.20 = 1.30 (value_real scale 4)
        assertThat(valueOf(ldl)).isEqualByComparingTo("1.30");
    }

    @Test
    void blanks_formula_when_an_input_is_missing() {
        var ct = measured("CT", "01KQZ0WPVQA364NMRAJMKRQCA1", "2.00");
        // HDL not entered (no measured result for it)
        var ldl = formula("LDL", "01KQZ0WPVQA364NMRAJMKRQCA4", "CT - HDL");
        when(repo.findActiveByLabFileId(LF)).thenReturn(List.of(ct, ldl));

        service.recompute(LF);

        assertThat(valueOf(ldl)).isNull();
    }

    @Test
    void resolves_formula_referencing_another_formula() {
        var ct = measured("CT", "01KQZ0WPVQA364NMRAJMKRQCA1", "5");
        var a = formula("A", "01KQZ0WPVQA364NMRAJMKRQCA4", "CT * 2"); // 10
        var b = formula("B", "01KQZ0WPVQA364NMRAJMKRQCA5", "A + 1"); // 11
        when(repo.findActiveByLabFileId(LF)).thenReturn(List.of(ct, a, b));

        service.recompute(LF);

        assertThat(valueOf(a)).isEqualByComparingTo("10");
        assertThat(valueOf(b)).isEqualByComparingTo("11");
    }

    @Test
    void cycle_leaves_both_formulas_blank() {
        var a = formula("A", "01KQZ0WPVQA364NMRAJMKRQCA4", "B + 1");
        var b = formula("B", "01KQZ0WPVQA364NMRAJMKRQCA5", "A + 1");
        when(repo.findActiveByLabFileId(LF)).thenReturn(List.of(a, b));

        service.recompute(LF);

        assertThat(valueOf(a)).isNull();
        assertThat(valueOf(b)).isNull();
    }

    @Test
    void divide_by_zero_blanks_formula() {
        var ct = measured("CT", "01KQZ0WPVQA364NMRAJMKRQCA1", "1");
        var z = measured("Z", "01KQZ0WPVQA364NMRAJMKRQCA2", "0");
        var ldl = formula("R", "01KQZ0WPVQA364NMRAJMKRQCA4", "CT / Z");
        when(repo.findActiveByLabFileId(LF)).thenReturn(List.of(ct, z, ldl));

        service.recompute(LF);

        assertThat(valueOf(ldl)).isNull();
    }

    @Test
    void does_not_rewrite_a_validated_formula_result() {
        var ct = measured("CT", "01KQZ0WPVQA364NMRAJMKRQCA1", "5");
        var ldl = formula("LDL", "01KQZ0WPVQA364NMRAJMKRQCA4", "CT * 2");
        when(repo.findActiveByLabFileId(LF)).thenReturn(List.of(ct, ldl));
        service.recompute(LF);
        assertThat(valueOf(ldl)).isEqualByComparingTo("10");

        // Validate (lock) the formula result — allowed while the dossier itself is still editable.
        ldl.validateTechnical("tech-user", FIXED);
        ldl.validateBiological("bio-user", FIXED);

        // Editing a sibling input must NOT throw ResultLockedException and must NOT rewrite the
        // frozen formula value (its inputs are seeded so dependents still resolve).
        var ctLine = ct.getLines().iterator().next();
        ct.setLineValue(
                ctLine.resultLineId(),
                new NewLineValue(new NumericResult(new BigDecimal("6")), null),
                null);
        service.recompute(LF);

        assertThat(valueOf(ldl)).isEqualByComparingTo("10"); // frozen, not recomputed to 12
    }

    @Test
    void unconfigured_formula_expression_blanks_the_line() {
        var ldl = result("LDL", "01KQZ0WPVQA364NMRAJMKRQCA4", ResultType.FORMULA);
        var a = mock(AnalysisSummary.class);
        when(a.formulaExpression()).thenReturn(null);
        when(analysisApi.findById(any())).thenReturn(Optional.of(a));
        when(repo.findActiveByLabFileId(LF)).thenReturn(List.of(ldl));

        service.recompute(LF);

        assertThat(valueOf(ldl)).isNull();
    }
}
