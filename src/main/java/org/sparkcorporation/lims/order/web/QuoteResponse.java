package org.sparkcorporation.lims.order.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.QuoteStatus;
import org.sparkcorporation.lims.shared.domain.Sex;

/**
 * Web-tier mirror of {@link org.sparkcorporation.lims.order.api.QuoteSummary}. Carries flat
 * snapshots of the linked patient / prescriber, the line list, and the locked-pricing fields (zero
 * pre-{@code :lock}). Mirrors {@link LabFileResponse} shape.
 */
@Schema(
        description =
                "A price quote (devis): a draft list of priced analyses for a patient, that can be"
                        + " locked and later converted into a lab file.")
public record QuoteResponse(
        @Schema(description = "Unique identifier of the quote.") String id,
        @Schema(
                        description =
                                "Human-readable quote number assigned when the quote was opened;"
                                        + " immutable.")
                String quoteNumber,
        @Schema(description = "Flat snapshot of the patient this quote is drawn up for.")
                PatientSnapshotResponse patient,
        @Schema(description = "Flat snapshot of the prescribing doctor, if any.")
                @Nullable PrescriberSnapshotResponse prescriber,
        @Schema(
                        description =
                                "Current lifecycle status of the quote (DRAFT, LOCKED, CONVERTED,"
                                        + " EXPIRED).")
                QuoteStatus status,
        @Schema(
                        description =
                                "Timestamp at which the quote was priced and locked (immutable from"
                                        + " then on), if it has been locked.")
                @Nullable Instant lockedAt,
        @Schema(
                        description =
                                "Identifier of the lab file created from this quote via :convert,"
                                        + " if it has been converted.")
                @Nullable String labFileId,
        @Schema(
                        description =
                                "Timestamp at which the quote was converted into a lab file, if"
                                        + " applicable.")
                @Nullable Instant convertedAt,
        @Schema(description = "Date after which this quote is no longer valid, if any.")
                @Nullable Instant expiresAt,
        @Schema(
                        description =
                                "Total quote amount in XOF; zero until the quote is locked and"
                                        + " priced.")
                BigDecimal totalAmount,
        @Schema(description = "Quoted analysis lines.") List<QuoteLineResponse> lines,
        @Schema(description = "Timestamp at which the quote was opened.") Instant createdAt,
        @Schema(description = "Identifier of the staff user who opened the quote, if known.")
                @Nullable String createdByUserId) {

    public QuoteResponse {
        lines = List.copyOf(lines);
    }

    /** Flat snapshot of the linked patient — never null (FK NOT NULL). */
    @Schema(description = "Flat snapshot of a patient, as captured on the quote.")
    public record PatientSnapshotResponse(
            @Schema(description = "Identifier of the patient.") String id,
            @Schema(description = "Lab-internal patient code.") String labCode,
            @Schema(description = "Patient's last name.") String lastName,
            @Schema(description = "Patient's first name(s), if recorded.")
                    @Nullable String firstNames,
            @Schema(description = "Patient's sex.") Sex sex) {}

    /** Flat snapshot of the linked prescriber — nullable. */
    @Schema(description = "Flat snapshot of a prescribing doctor, as captured on the quote.")
    public record PrescriberSnapshotResponse(
            @Schema(description = "Identifier of the prescriber.") String id,
            @Schema(description = "Lab-internal prescriber code.") String code,
            @Schema(description = "Prescriber's full name.") String fullName,
            @Schema(description = "Prescriber's medical specialty code, if recorded.")
                    @Nullable String specialtyCode) {}

    /** One quote line; pricing fields populated post-{@code :lock} (zero before). */
    @Schema(
            description =
                    "One catalog analysis quoted on the quote. Pricing fields (bRateSnapshot,"
                            + " lineTotal) stay at zero until the quote is locked.")
    public record QuoteLineResponse(
            @Schema(description = "Unique identifier of this quote line.") String id,
            @Schema(description = "Identifier of the catalog analysis quoted.") String analysisId,
            @Schema(
                            description =
                                    "Catalog code of the quoted analysis, as resolved at line"
                                            + " creation.")
                    String analysisCode,
            @Schema(
                            description =
                                    "Snapshot of the analysis's indiciary value B (billing"
                                            + " coefficient), captured when the line was created.")
                    BigDecimal indiciaryValueBSnapshot,
            @Schema(
                            description =
                                    "Snapshot of the applicable rate for value B, in XOF; written"
                                            + " once at :lock time (zero before).")
                    BigDecimal bRateSnapshot,
            @Schema(
                            description =
                                    "Snapshot of the number of labels to print for this analysis,"
                                            + " captured when the line was created.")
                    int labelCountSnapshot,
            @Schema(
                            description =
                                    "Price of this line in XOF (indiciaryValueBSnapshot ×"
                                        + " bRateSnapshot × labelCountSnapshot); written once at"
                                        + " :lock time (zero before).")
                    BigDecimal lineTotal) {}
}
