package org.sparkcorporation.lims.order.infra.pdf;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.labsettings.api.LabSettingsApi;
import org.sparkcorporation.lims.order.api.QuoteApi;
import org.sparkcorporation.lims.order.api.QuoteId;
import org.sparkcorporation.lims.order.api.QuoteStatus;
import org.sparkcorporation.lims.order.api.QuoteSummary;

/**
 * Builds a {@link QuoteReportModel} from a quote id. Reads via {@link QuoteApi#requireById} (which
 * throws {@code QuoteNotFoundException} on miss — propagates to the shared 404 advice) and {@link
 * LabSettingsApi#get()}. Stateless, transactional read-only.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteReportModelAssembler {

    private static final DateTimeFormatter FRENCH_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH).withZone(ZoneId.of("UTC"));
    private static final String NOT_PRICED = "À déterminer";

    private final QuoteApi quoteApi;
    private final LabSettingsApi labSettingsApi;

    public QuoteReportModel assemble(QuoteId quoteId) {
        var quote = quoteApi.requireById(quoteId);
        var lab = labSettingsApi.get();
        boolean draft = quote.status() == QuoteStatus.DRAFT;

        return new QuoteReportModel(
                new QuoteReportModel.QuoteHeader(
                        quote.id().value(),
                        quote.quoteNumber().value(),
                        FRENCH_DATE.format(quote.createdAt()),
                        quote.createdAt(),
                        quote.lockedAt() == null ? null : FRENCH_DATE.format(quote.lockedAt()),
                        quote.convertedAt() == null
                                ? null
                                : FRENCH_DATE.format(quote.convertedAt()),
                        quote.expiresAt() == null ? null : FRENCH_DATE.format(quote.expiresAt()),
                        quote.status()),
                new QuoteReportModel.PatientSummary(
                        quote.patient().lastName(),
                        quote.patient().firstNames() == null ? "" : quote.patient().firstNames(),
                        quote.patient().sex().name(),
                        quote.patient().labCode()),
                quote.prescriber() == null
                        ? null
                        : new QuoteReportModel.PrescriberSummary(
                                quote.prescriber().fullName(), quote.prescriber().specialtyCode()),
                statusBanner(quote.status()),
                quote.lines().stream().map(line -> toLineRow(line, draft)).toList(),
                new QuoteReportModel.TotalsBlock(
                        draft ? NOT_PRICED : amountDisplay(quote.totalAmount())),
                new QuoteReportModel.LabIdentity(
                        lab.name(), lab.address(),
                        lab.accreditationNumber(), lab.managerName()),
                Locale.FRENCH);
    }

    private static QuoteReportModel.LineRow toLineRow(
            QuoteSummary.QuoteLineSummary line, boolean draft) {
        if (draft) {
            return new QuoteReportModel.LineRow(
                    line.analysisCode(),
                    line.analysisCode(),
                    NOT_PRICED,
                    line.labelCountSnapshot(),
                    NOT_PRICED);
        }
        return new QuoteReportModel.LineRow(
                line.analysisCode(),
                line.analysisCode(),
                bRateDisplay(line.bRateSnapshot()),
                line.labelCountSnapshot(),
                amountDisplay(line.lineTotal()));
    }

    static QuoteReportModel.StatusBanner statusBanner(QuoteStatus status) {
        return switch (status) {
            case DRAFT -> new QuoteReportModel.StatusBanner("BROUILLON", "non contractuel", true);
            case LOCKED -> new QuoteReportModel.StatusBanner("DEVIS", null, false);
            case CONVERTED ->
                    new QuoteReportModel.StatusBanner("DEVIS", "converti en dossier", false);
            case EXPIRED -> new QuoteReportModel.StatusBanner("DEVIS", "expiré", false);
        };
    }

    static String bRateDisplay(BigDecimal bRate) {
        var fr = NumberFormat.getInstance(Locale.FRENCH);
        fr.setGroupingUsed(false);
        fr.setMinimumFractionDigits(0);
        fr.setMaximumFractionDigits(2);
        return "B" + fr.format(bRate);
    }

    static String amountDisplay(BigDecimal amount) {
        var fr = NumberFormat.getInstance(Locale.FRENCH);
        fr.setGroupingUsed(true);
        fr.setMinimumFractionDigits(0);
        fr.setMaximumFractionDigits(0);
        return fr.format(amount) + " F CFA";
    }
}
