package org.sparkcorporation.lims.order.domain;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.DiscountKind;

/**
 * Whitelist patch payload for {@link LabFile#update(UpdateLabFile, java.time.Clock)}. Each field is
 * nullable; a {@code null} sentinel means "no change". Mutable fields: {@code urgent}, {@code
 * prescriberId}, {@code preleveurId}, plus the 1.6.a billing block ({@code insuranceId}, {@code
 * partnerId}, {@code homeCollectionFee}, {@code additionalFees}, {@code discountKind}, {@code
 * discountValue}). Registration number, patient, and status raise dedicated immutability exceptions
 * higher up.
 */
public record UpdateLabFile(
        @Nullable Boolean urgent,
        @Nullable String prescriberId,
        @Nullable String preleveurId,
        @Nullable String insuranceId,
        @Nullable String partnerId,
        @Nullable BigDecimal homeCollectionFee,
        @Nullable BigDecimal additionalFees,
        @Nullable DiscountKind discountKind,
        @Nullable BigDecimal discountValue) {}
