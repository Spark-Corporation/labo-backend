package org.sparkcorporation.lims.billing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.DiscountKind;

public final class PricingEngine {

    private static final RoundingMode ROUND = RoundingMode.HALF_UP;
    private static final int XOF_SCALE = 0;

    public PricingResult price(
            List<LineInput> lines,
            BigDecimal homeCollectionFee,
            BigDecimal additionalFees,
            @Nullable DiscountKind discountKind,
            @Nullable BigDecimal discountValue,
            PricingContext context) {
        Objects.requireNonNull(lines, "lines");
        Objects.requireNonNull(homeCollectionFee, "homeCollectionFee");
        Objects.requireNonNull(additionalFees, "additionalFees");
        Objects.requireNonNull(context, "context");
        if ((discountKind == null) != (discountValue == null)) {
            throw new InvalidPricingException(
                    "discountKind and discountValue must both be set or both null");
        }

        BigDecimal bRate = bRateFor(context);
        List<PricingResult.LineResult> lineResults = new ArrayList<>();
        BigDecimal lineSubtotal = BigDecimal.ZERO;
        int position = 1;
        for (LineInput in : lines) {
            BigDecimal lineTotal =
                    in.indiciaryValueB()
                            .multiply(bRate)
                            .multiply(BigDecimal.valueOf(in.labelCount()))
                            .setScale(XOF_SCALE, ROUND);
            lineResults.add(new PricingResult.LineResult(position++, bRate, lineTotal));
            lineSubtotal = lineSubtotal.add(lineTotal);
        }
        BigDecimal subtotal = lineSubtotal.add(homeCollectionFee).add(additionalFees);

        BigDecimal coverage =
                switch (context) {
                    case PricingContext.SelfPay selfPay -> BigDecimal.ZERO;
                    case PricingContext.Insured ins ->
                            subtotal.multiply(ins.coverageRate()).setScale(XOF_SCALE, ROUND);
                    case PricingContext.PartnerRouted pr -> subtotal;
                };
        BigDecimal patientShare = subtotal.subtract(coverage);

        BigDecimal discount =
                discountKind == null
                        ? BigDecimal.ZERO
                        : switch (discountKind) {
                            case AMOUNT -> patientShare.min(discountValue);
                            case PERCENT ->
                                    patientShare.multiply(discountValue).setScale(XOF_SCALE, ROUND);
                        };

        BigDecimal net = patientShare.subtract(discount);
        // Defensive: AMOUNT is capped at patientShare; PERCENT is bounded by the [0,1] CHECK
        // constraint. The throw guards against a future refactor that loosens those bounds.
        if (net.signum() < 0) {
            throw new InvalidPricingException(
                    "net amount went negative — discount + coverage exceeds total");
        }

        return new PricingResult(
                subtotal.setScale(XOF_SCALE, ROUND),
                coverage,
                discount,
                net.setScale(XOF_SCALE, ROUND),
                List.copyOf(lineResults));
    }

    private BigDecimal bRateFor(PricingContext context) {
        return switch (context) {
            case PricingContext.SelfPay s -> s.labDefaultBRate();
            case PricingContext.Insured i -> i.insuranceBRate();
            case PricingContext.PartnerRouted p -> p.partnerBRate();
        };
    }

    public record LineInput(BigDecimal indiciaryValueB, int labelCount) {
        public LineInput {
            Objects.requireNonNull(indiciaryValueB, "indiciaryValueB");
            if (labelCount < 0) {
                throw new IllegalArgumentException("labelCount must be >= 0");
            }
        }
    }

    public sealed interface PricingContext
            permits PricingContext.SelfPay, PricingContext.Insured, PricingContext.PartnerRouted {
        record SelfPay(BigDecimal labDefaultBRate) implements PricingContext {}

        record Insured(BigDecimal insuranceBRate, BigDecimal coverageRate)
                implements PricingContext {}

        record PartnerRouted(BigDecimal partnerBRate) implements PricingContext {}
    }
}
