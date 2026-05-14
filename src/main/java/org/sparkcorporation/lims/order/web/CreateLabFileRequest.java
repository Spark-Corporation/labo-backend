package org.sparkcorporation.lims.order.web;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.DiscountKind;

/**
 * POST {@code /api/v1/lab-files} payload — opens a new lab file (dossier) at status {@code OPEN}.
 *
 * <p>{@code patientId} required; {@code prescriberId} / {@code preleveurId} optional. {@code
 * analyses} is a non-empty list of analysis codes (catalog reference) that the service resolves via
 * {@code AnalysisApi.findByCode} and dedup-checks before bumping the registration counter.
 *
 * <p>1.6.a billing additions: {@code insuranceId} / {@code partnerId} optional — when {@code null}
 * the service defaults from the patient's own insurance/partner FKs. (Known partial: this collapses
 * "field absent" and "field present-and-null" into the same semantics for 1.6.a; the explicit-null
 * "force-clear" intent waits for a richer payload shape.) {@code homeCollectionFee} / {@code
 * additionalFees} default to zero when {@code null}; the {@code discount*} pair is either both null
 * (no discount) or both set (validated by the aggregate).
 */
public record CreateLabFileRequest(
        @NotBlank @Size(max = 26) String patientId,
        @Size(max = 26) @Nullable String prescriberId,
        @Size(max = 26) @Nullable String preleveurId,
        boolean urgent,
        @NotEmpty @Size(max = 50) List<@NotBlank @Size(max = 32) String> analyses,
        @Size(max = 26) @Nullable String insuranceId,
        @Size(max = 26) @Nullable String partnerId,
        @Nullable BigDecimal homeCollectionFee,
        @Nullable BigDecimal additionalFees,
        @Nullable DiscountKind discountKind,
        @Nullable BigDecimal discountValue) {}
