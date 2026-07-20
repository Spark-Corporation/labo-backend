package org.sparkcorporation.lims.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.sparkcorporation.lims.billing.domain.PricingEngine.LineInput;
import org.sparkcorporation.lims.billing.domain.PricingEngine.PricingContext;
import org.sparkcorporation.lims.billing.domain.PricingEngine.PricingContext.Insured;
import org.sparkcorporation.lims.billing.domain.PricingEngine.PricingContext.PartnerRouted;
import org.sparkcorporation.lims.billing.domain.PricingEngine.PricingContext.SelfPay;
import org.sparkcorporation.lims.order.api.DiscountKind;

class PricingEngineTest {

    private final PricingEngine engine = new PricingEngine();

    @Test
    void insured_with_amount_discount_returns_patient_share_minus_discount() {
        // Two analyses: 50B × 1500 × 1 = 75000, 30B × 1500 × 1 = 45000. Subtotal = 120000.
        // Insurance covers 80%: coverage = 96000. Patient share = 24000.
        // Amount discount = 5000. Net = 19000.
        PricingResult result =
                engine.price(
                        List.of(
                                new LineInput(new BigDecimal("50"), 1),
                                new LineInput(new BigDecimal("30"), 1)),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        DiscountKind.AMOUNT,
                        new BigDecimal("5000"),
                        new Insured(new BigDecimal("1500"), new BigDecimal("0.80")));

        assertThat(result.totalAmount()).isEqualByComparingTo("120000");
        assertThat(result.insuranceCoverageAmount()).isEqualByComparingTo("96000");
        assertThat(result.discountAmount()).isEqualByComparingTo("5000");
        assertThat(result.netAmount()).isEqualByComparingTo("19000");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void price_matches_expected(
            String label,
            List<LineInput> lines,
            BigDecimal homeCollectionFee,
            BigDecimal additionalFees,
            @Nullable DiscountKind discountKind,
            @Nullable BigDecimal discountValue,
            PricingContext context,
            String expectedTotal,
            String expectedCoverage,
            String expectedDiscount,
            String expectedNet) {
        PricingResult result =
                engine.price(
                        lines,
                        homeCollectionFee,
                        additionalFees,
                        discountKind,
                        discountValue,
                        context);

        assertThat(result.totalAmount()).isEqualByComparingTo(expectedTotal);
        assertThat(result.insuranceCoverageAmount()).isEqualByComparingTo(expectedCoverage);
        assertThat(result.discountAmount()).isEqualByComparingTo(expectedDiscount);
        assertThat(result.netAmount()).isEqualByComparingTo(expectedNet);
    }

    static Stream<Arguments> cases() {
        return Stream.of(
                Arguments.of(
                        "selfpay_zero_lines_no_fees",
                        List.<LineInput>of(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        new SelfPay(new BigDecimal("1000")),
                        "0",
                        "0",
                        "0",
                        "0"),
                Arguments.of(
                        "selfpay_amount_discount_caps_at_patient_share",
                        List.of(new LineInput(new BigDecimal("10"), 1)),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        DiscountKind.AMOUNT,
                        new BigDecimal("5000"),
                        new SelfPay(new BigDecimal("100")),
                        "1000",
                        "0",
                        "1000",
                        "0"),
                Arguments.of(
                        "selfpay_percent_50",
                        List.of(new LineInput(new BigDecimal("20"), 1)),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        DiscountKind.PERCENT,
                        new BigDecimal("0.50"),
                        new SelfPay(new BigDecimal("1000")),
                        "20000",
                        "0",
                        "10000",
                        "10000"),
                Arguments.of(
                        "selfpay_percent_zero_no_discount",
                        List.of(new LineInput(new BigDecimal("20"), 1)),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        DiscountKind.PERCENT,
                        new BigDecimal("0.00"),
                        new SelfPay(new BigDecimal("1000")),
                        "20000",
                        "0",
                        "0",
                        "20000"),
                Arguments.of(
                        "selfpay_percent_100_full_discount",
                        List.of(new LineInput(new BigDecimal("20"), 1)),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        DiscountKind.PERCENT,
                        new BigDecimal("1.00"),
                        new SelfPay(new BigDecimal("1000")),
                        "20000",
                        "0",
                        "20000",
                        "0"),
                Arguments.of(
                        "selfpay_with_home_and_additional_fees",
                        List.of(new LineInput(new BigDecimal("10"), 1)),
                        new BigDecimal("2000"),
                        new BigDecimal("500"),
                        null,
                        null,
                        new SelfPay(new BigDecimal("1000")),
                        "12500",
                        "0",
                        "0",
                        "12500"),
                Arguments.of(
                        "insured_0_pct_acts_like_selfpay",
                        List.of(new LineInput(new BigDecimal("10"), 1)),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        new Insured(new BigDecimal("1500"), new BigDecimal("0.00")),
                        "15000",
                        "0",
                        "0",
                        "15000"),
                Arguments.of(
                        "insured_100_pct_patient_pays_zero",
                        List.of(new LineInput(new BigDecimal("10"), 1)),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        new Insured(new BigDecimal("1500"), new BigDecimal("1.00")),
                        "15000",
                        "15000",
                        "0",
                        "0"),
                Arguments.of(
                        "insured_50_pct_amount_discount_on_patient_half",
                        List.of(new LineInput(new BigDecimal("10"), 1)),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        DiscountKind.AMOUNT,
                        new BigDecimal("2000"),
                        new Insured(new BigDecimal("1500"), new BigDecimal("0.50")),
                        "15000",
                        "7500",
                        "2000",
                        "5500"),
                Arguments.of(
                        "insured_80_pct_percent_10_with_home_fee",
                        List.of(new LineInput(new BigDecimal("50"), 1)),
                        new BigDecimal("2000"),
                        BigDecimal.ZERO,
                        DiscountKind.PERCENT,
                        new BigDecimal("0.10"),
                        new Insured(new BigDecimal("1500"), new BigDecimal("0.80")),
                        "77000",
                        "61600",
                        "1540",
                        "13860"),
                Arguments.of(
                        "partner_routed_patient_pays_zero",
                        List.of(new LineInput(new BigDecimal("30"), 1)),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        new PartnerRouted(new BigDecimal("2000")),
                        "60000",
                        "60000",
                        "0",
                        "0"),
                Arguments.of(
                        "partner_routed_amount_discount_caps_at_zero",
                        List.of(new LineInput(new BigDecimal("30"), 1)),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        DiscountKind.AMOUNT,
                        new BigDecimal("1000"),
                        new PartnerRouted(new BigDecimal("2000")),
                        "60000",
                        "60000",
                        "0",
                        "0"),
                Arguments.of(
                        "empty_lines_with_fees_only",
                        List.<LineInput>of(),
                        new BigDecimal("1000"),
                        new BigDecimal("500"),
                        null,
                        null,
                        new SelfPay(new BigDecimal("1000")),
                        "1500",
                        "0",
                        "0",
                        "1500"),
                Arguments.of(
                        "label_count_zero_yields_zero_line_total",
                        List.of(new LineInput(new BigDecimal("10"), 0)),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        new SelfPay(new BigDecimal("1000")),
                        "0",
                        "0",
                        "0",
                        "0"),
                Arguments.of(
                        "large_values_no_overflow",
                        List.of(new LineInput(new BigDecimal("1000000"), 100)),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        new SelfPay(new BigDecimal("1000000")),
                        "100000000000000",
                        "0",
                        "0",
                        "100000000000000"),
                Arguments.of(
                        "selfpay_additional_fees_only",
                        List.<LineInput>of(),
                        BigDecimal.ZERO,
                        new BigDecimal("3000"),
                        null,
                        null,
                        new SelfPay(new BigDecimal("1000")),
                        "3000",
                        "0",
                        "0",
                        "3000"),
                Arguments.of(
                        "insured_50_pct_additional_only_coverage_applies",
                        List.<LineInput>of(),
                        BigDecimal.ZERO,
                        new BigDecimal("3000"),
                        null,
                        null,
                        new Insured(new BigDecimal("1500"), new BigDecimal("0.50")),
                        "3000",
                        "1500",
                        "0",
                        "1500"),
                Arguments.of(
                        "insured_50_pct_home_only_coverage_applies",
                        List.<LineInput>of(),
                        new BigDecimal("4000"),
                        BigDecimal.ZERO,
                        null,
                        null,
                        new Insured(new BigDecimal("1500"), new BigDecimal("0.50")),
                        "4000",
                        "2000",
                        "0",
                        "2000"),
                Arguments.of(
                        "selfpay_uses_lab_default_b_rate",
                        List.of(new LineInput(new BigDecimal("10"), 1)),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        new SelfPay(new BigDecimal("750")),
                        "7500",
                        "0",
                        "0",
                        "7500"),
                Arguments.of(
                        "insured_uses_insurance_b_rate",
                        List.of(new LineInput(new BigDecimal("10"), 1)),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        new Insured(new BigDecimal("800"), new BigDecimal("0.00")),
                        "8000",
                        "0",
                        "0",
                        "8000"),
                Arguments.of(
                        "partner_routed_uses_partner_b_rate",
                        List.of(new LineInput(new BigDecimal("10"), 1)),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        new PartnerRouted(new BigDecimal("900")),
                        "9000",
                        "9000",
                        "0",
                        "0"),
                Arguments.of(
                        "selfpay_two_lines_aggregate",
                        List.of(
                                new LineInput(new BigDecimal("10"), 2),
                                new LineInput(new BigDecimal("20"), 1)),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        new SelfPay(new BigDecimal("1000")),
                        "40000",
                        "0",
                        "0",
                        "40000"));
    }

    @Test
    void throws_when_discount_kind_set_but_value_null() {
        assertThatThrownBy(
                        () ->
                                engine.price(
                                        List.of(new LineInput(new BigDecimal("10"), 1)),
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO,
                                        DiscountKind.AMOUNT,
                                        null,
                                        new SelfPay(new BigDecimal("1000"))))
                .isInstanceOf(InvalidPricingException.class)
                .hasMessageContaining("both be set or both null");
    }

    @Test
    void throws_when_discount_value_set_but_kind_null() {
        assertThatThrownBy(
                        () ->
                                engine.price(
                                        List.of(new LineInput(new BigDecimal("10"), 1)),
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO,
                                        null,
                                        new BigDecimal("500"),
                                        new SelfPay(new BigDecimal("1000"))))
                .isInstanceOf(InvalidPricingException.class)
                .hasMessageContaining("both be set or both null");
    }
}
