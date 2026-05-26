package org.sparkcorporation.lims.order.infra.pdf;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.QuoteStatus;

/**
 * Pure-data model for the quote (devis) PDF. Built by {@link QuoteReportModelAssembler} from {@code
 * QuoteApi.requireById} + {@code LabSettingsApi.get()}; consumed by {@code order/quote-report.html}
 * via {@code OpenHtmlToPdfRenderingService}.
 *
 * <p>All formatting (French dates, bRate display, amount display) is computed by the assembler so
 * the template stays declarative.
 */
public record QuoteReportModel(
        QuoteHeader header,
        PatientSummary patient,
        @Nullable PrescriberSummary prescriber,
        StatusBanner statusBanner,
        List<LineRow> lines,
        TotalsBlock totals,
        LabIdentity labIdentity,
        Locale locale) {

    public QuoteReportModel {
        lines = List.copyOf(lines);
    }

    /** {@code createdAtUtc} is the raw instant used by {@code defaultFilename} for yyyyMMdd. */
    public record QuoteHeader(
            String quoteId,
            String quoteNumber,
            String createdAtDisplay,
            Instant createdAtUtc,
            @Nullable String lockedAtDisplay,
            @Nullable String convertedAtDisplay,
            @Nullable String expiresAtDisplay,
            QuoteStatus status) {}

    public record PatientSummary(String lastName, String firstName, String sex, String labCode) {}

    public record PrescriberSummary(String fullName, @Nullable String specialtyCode) {}

    /** {@code subtitle} is the optional legal-status caveat ("non contractuel" for drafts). */
    public record StatusBanner(String title, @Nullable String subtitle, boolean isDraft) {}

    public record LineRow(
            String code,
            String label,
            String bRateDisplay,
            int labelCount,
            String lineTotalDisplay) {}

    public record TotalsBlock(String totalAmountDisplay) {}

    public record LabIdentity(
            String name, String address, String accreditationNumber, String managerName) {}
}
