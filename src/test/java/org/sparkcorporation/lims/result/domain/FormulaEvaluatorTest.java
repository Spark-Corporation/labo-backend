package org.sparkcorporation.lims.result.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;

class FormulaEvaluatorTest {

    private final FormulaEvaluator evaluator = new FormulaEvaluator();

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    @Test
    void evaluates_ldl_friedewald_across_codes() {
        var values = Map.of("CT", bd("2.00"), "HDL", bd("0.50"), "TG", bd("1.00"));
        // LDL = CT - HDL - TG/5 = 2.00 - 0.50 - 0.20 = 1.30
        assertThat(evaluator.evaluate("CT - HDL - TG/5", values)).contains(bd("1.30"));
    }

    @Test
    void respects_precedence_and_parentheses() {
        var v = Map.of("A", bd("2"), "B", bd("3"), "C", bd("4"));
        assertThat(evaluator.evaluate("A + B * C", v)).contains(bd("14"));
        assertThat(evaluator.evaluate("(A + B) * C", v)).contains(bd("20"));
    }

    @Test
    void handles_unary_minus_and_decimal_literals() {
        assertThat(evaluator.evaluate("-3 + 1.5", Map.of())).contains(bd("-1.5"));
    }

    @Test
    void unknown_code_yields_empty() {
        assertThat(evaluator.evaluate("CT - MISSING", Map.of("CT", bd("2")))).isEmpty();
    }

    @Test
    void divide_by_zero_yields_empty() {
        assertThat(evaluator.evaluate("A / B", Map.of("A", bd("1"), "B", bd("0")))).isEmpty();
    }

    @Test
    void malformed_expression_yields_empty() {
        assertThat(evaluator.evaluate("CT - * HDL", Map.of("CT", bd("2"), "HDL", bd("1"))))
                .isEmpty();
        assertThat(evaluator.evaluate("", Map.of())).isEmpty();
        assertThat(evaluator.evaluate("(A + B", Map.of("A", bd("1"), "B", bd("2")))).isEmpty();
    }

    @Test
    void referencedCodes_extracts_identifiers() {
        assertThat(evaluator.referencedCodes("CT - HDL - TG/5"))
                .containsExactlyInAnyOrder("CT", "HDL", "TG");
        assertThat(evaluator.referencedCodes("1 + 2")).isEmpty();
    }
}
