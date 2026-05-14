package org.sparkcorporation.lims.billing.domain;

import java.math.BigDecimal;
import java.util.List;

public record PricingResult(
        BigDecimal totalAmount,
        BigDecimal insuranceCoverageAmount,
        BigDecimal discountAmount,
        BigDecimal netAmount,
        List<LineResult> lines) {

    public PricingResult {
        lines = List.copyOf(lines);
    }

    public record LineResult(int position, BigDecimal bRate, BigDecimal lineTotal) {}
}
