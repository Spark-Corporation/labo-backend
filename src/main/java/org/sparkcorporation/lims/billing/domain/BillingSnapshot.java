package org.sparkcorporation.lims.billing.domain;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.DiscountKind;

/**
 * Per-file billing inputs captured at invoice issuance (from {@code lab_files.home_collection_fee /
 * additional_fees / discount_*}). Spec §5.1: written verbatim to the {@code *_snapshot} columns on
 * the invoice — never recomputed, so the invoice survives subsequent lab-file edits unchanged.
 */
public record BillingSnapshot(
        BigDecimal homeCollectionFee,
        BigDecimal additionalFees,
        @Nullable DiscountKind discountKind,
        @Nullable BigDecimal discountValue) {

    public BillingSnapshot {
        if (homeCollectionFee == null) {
            throw new IllegalArgumentException("homeCollectionFee must not be null");
        }
        if (additionalFees == null) {
            throw new IllegalArgumentException("additionalFees must not be null");
        }
        if ((discountKind == null) != (discountValue == null)) {
            throw new IllegalArgumentException(
                    "discountKind and discountValue must both be set or both null");
        }
    }
}
