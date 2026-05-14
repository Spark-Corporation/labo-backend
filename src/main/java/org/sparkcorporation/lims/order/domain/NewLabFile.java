package org.sparkcorporation.lims.order.domain;

import java.math.BigDecimal;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.DiscountKind;

/**
 * Command record passed to {@link LabFile#open(NewLabFile,
 * org.sparkcorporation.lims.order.api.LabFileRegistrationNumber, String, java.time.Clock)}. The
 * compact constructor copies {@code analysisCodes} via {@link List#copyOf(java.util.Collection)} so
 * the aggregate is insulated from caller-side mutations.
 *
 * <p>1.6.a additions: insurance/partner FKs + billing block (fees + optional discount). All billing
 * fields are nullable on the command — the service resolves defaults from the patient. The compact
 * constructor coerces null fee values to {@link BigDecimal#ZERO} so the entity never sees a null in
 * the NOT-NULL fee columns.
 */
public record NewLabFile(
        String patientId,
        @Nullable String prescriberId,
        @Nullable String preleveurId,
        boolean urgent,
        List<String> analysisCodes,
        @Nullable String insuranceId,
        @Nullable String partnerId,
        BigDecimal homeCollectionFee,
        BigDecimal additionalFees,
        @Nullable DiscountKind discountKind,
        @Nullable BigDecimal discountValue) {

    public NewLabFile {
        analysisCodes = List.copyOf(analysisCodes);
        if (homeCollectionFee == null) {
            homeCollectionFee = BigDecimal.ZERO;
        }
        if (additionalFees == null) {
            additionalFees = BigDecimal.ZERO;
        }
    }
}
