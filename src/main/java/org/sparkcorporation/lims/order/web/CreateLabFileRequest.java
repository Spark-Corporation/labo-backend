package org.sparkcorporation.lims.order.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request to open a new lab file (dossier) for a patient, at status OPEN.")
public record CreateLabFileRequest(
        @Schema(description = "Identifier of the patient this lab file is opened for. Required.")
                @NotBlank
                @Size(max = 26)
                String patientId,
        @Schema(description = "Identifier of the prescribing doctor, if any.") @Size(max = 26)
                @Nullable String prescriberId,
        @Schema(
                        description =
                                "Identifier of the Préleveur (home-collection agent) assigned to"
                                        + " this dossier, if any.")
                @Size(max = 26)
                @Nullable String preleveurId,
        @Schema(description = "Whether this dossier is flagged urgent (rush processing).")
                boolean urgent,
        @Schema(
                        description =
                                "Catalog analysis codes to request on this dossier; resolved and"
                                        + " dedup-checked before persistence. Must not be empty.")
                @NotEmpty
                @Size(max = 50)
                List<@NotBlank @Size(max = 32) String> analyses,
        @Schema(
                        description =
                                "Identifier of the insurance to bill; when omitted, defaults to the"
                                        + " patient's own insurance.")
                @Size(max = 26)
                @Nullable String insuranceId,
        @Schema(
                        description =
                                "Identifier of the billing partner; when omitted, defaults to the"
                                        + " patient's own partner.")
                @Size(max = 26)
                @Nullable String partnerId,
        @Schema(description = "Flat home-collection fee in XOF; defaults to zero when omitted.")
                @Nullable BigDecimal homeCollectionFee,
        @Schema(description = "Additional flat fees in XOF; defaults to zero when omitted.")
                @Nullable BigDecimal additionalFees,
        @Schema(
                        description =
                                "Kind of discount to apply (AMOUNT = flat XOF, PERCENT = fraction"
                                    + " in [0,1]); must be set together with discountValue or left"
                                    + " both null.")
                @Nullable DiscountKind discountKind,
        @Schema(
                        description =
                                "Discount value, interpreted according to discountKind (flat XOF"
                                        + " amount, or a [0,1] fraction).")
                @Nullable BigDecimal discountValue,
        @Schema(description = "Scheduled appointment (rendez-vous) time for this dossier, if any.")
                @Nullable Instant rendezVousAt) {}
