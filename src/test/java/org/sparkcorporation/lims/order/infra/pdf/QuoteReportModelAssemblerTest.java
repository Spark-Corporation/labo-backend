package org.sparkcorporation.lims.order.infra.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.sparkcorporation.lims.labsettings.api.LabSettings;
import org.sparkcorporation.lims.labsettings.api.LabSettingsApi;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.order.api.QuoteApi;
import org.sparkcorporation.lims.order.api.QuoteId;
import org.sparkcorporation.lims.order.api.QuoteLineId;
import org.sparkcorporation.lims.order.api.QuoteNumber;
import org.sparkcorporation.lims.order.api.QuoteStatus;
import org.sparkcorporation.lims.order.api.QuoteSummary;
import org.sparkcorporation.lims.order.domain.QuoteNotFoundException;
import org.sparkcorporation.lims.shared.domain.Sex;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuoteReportModelAssemblerTest {

    // Valid 26-char Crockford Base32 ULIDs (alphabet excludes I, L, O, U).
    private static final String QUOTE_ULID = "01HZX00000000000000QV0TE00";
    private static final String QUOTE_NUMBER_VALUE = "DEV-260526-0001";
    private static final String LAB_FILE_ULID = "01HZX0000000000000000ABFE0";
    private static final String QUOTE_LINE_ULID = "01HZX0000000000000000QJNE0";
    private static final String PATIENT_ULID = "01HZX0000000000000000PATJ0";
    private static final String PRESCRIBER_ULID = "01HZX0000000000000000PRSC0";
    private static final String ANALYSIS_ULID = "01HZX0000000000000000ANAJ0";
    private static final String USER_ULID = "01HZX0000000000000000VSER0";

    @Mock private QuoteApi quoteApi;
    @Mock private LabSettingsApi labSettingsApi;

    @InjectMocks private QuoteReportModelAssembler assembler;

    @Test
    void assemble_LOCKED_populatesPricingFields() {
        when(quoteApi.requireById(new QuoteId(QUOTE_ULID)))
                .thenReturn(quoteSummary(QuoteStatus.LOCKED, new BigDecimal("1800")));
        stubLabSettings();

        var model = assembler.assemble(new QuoteId(QUOTE_ULID));

        assertThat(model.header().quoteNumber()).isEqualTo(QUOTE_NUMBER_VALUE);
        assertThat(model.statusBanner().title()).isEqualTo("DEVIS");
        assertThat(model.statusBanner().subtitle()).isNull();
        assertThat(model.statusBanner().isDraft()).isFalse();
        assertThat(model.lines()).hasSize(1);
        assertThat(model.lines().get(0).bRateDisplay()).isEqualTo("B450");
        assertThat(model.lines().get(0).lineTotalDisplay())
                .isEqualTo(
                        NumberFormat.getInstance(Locale.FRENCH).format(new BigDecimal("1800"))
                                + " F CFA");
        assertThat(model.totals().totalAmountDisplay())
                .isEqualTo(
                        NumberFormat.getInstance(Locale.FRENCH).format(new BigDecimal("1800"))
                                + " F CFA");
        assertThat(model.labIdentity().name()).isEqualTo("Lab Sparkcorp");
    }

    @Test
    void assemble_DRAFT_setsAllPricingToAdeterminer() {
        when(quoteApi.requireById(new QuoteId(QUOTE_ULID)))
                .thenReturn(quoteSummary(QuoteStatus.DRAFT, BigDecimal.ZERO));
        stubLabSettings();

        var model = assembler.assemble(new QuoteId(QUOTE_ULID));

        assertThat(model.statusBanner().title()).isEqualTo("BROUILLON");
        assertThat(model.statusBanner().subtitle()).isEqualTo("non contractuel");
        assertThat(model.statusBanner().isDraft()).isTrue();
        assertThat(model.lines().get(0).bRateDisplay()).isEqualTo("À déterminer");
        assertThat(model.lines().get(0).lineTotalDisplay()).isEqualTo("À déterminer");
        assertThat(model.totals().totalAmountDisplay()).isEqualTo("À déterminer");
    }

    @Test
    void assemble_CONVERTED_setsBannerSubtitleToConverti() {
        when(quoteApi.requireById(new QuoteId(QUOTE_ULID)))
                .thenReturn(quoteSummary(QuoteStatus.CONVERTED, new BigDecimal("1800")));
        stubLabSettings();

        var model = assembler.assemble(new QuoteId(QUOTE_ULID));

        assertThat(model.statusBanner().title()).isEqualTo("DEVIS");
        assertThat(model.statusBanner().subtitle()).isEqualTo("converti en dossier");
    }

    @Test
    void assemble_EXPIRED_setsBannerSubtitleToExpire() {
        when(quoteApi.requireById(new QuoteId(QUOTE_ULID)))
                .thenReturn(quoteSummary(QuoteStatus.EXPIRED, new BigDecimal("1800")));
        stubLabSettings();

        var model = assembler.assemble(new QuoteId(QUOTE_ULID));

        assertThat(model.statusBanner().subtitle()).isEqualTo("expiré");
    }

    @Test
    void assemble_omitsPrescriber_whenNull() {
        when(quoteApi.requireById(new QuoteId(QUOTE_ULID)))
                .thenReturn(quoteSummaryWithoutPrescriber());
        stubLabSettings();

        var model = assembler.assemble(new QuoteId(QUOTE_ULID));

        assertThat(model.prescriber()).isNull();
    }

    @Test
    void assemble_propagatesQuoteNotFoundException() {
        var id = new QuoteId(QUOTE_ULID);
        when(quoteApi.requireById(id)).thenThrow(new QuoteNotFoundException(id));

        assertThatThrownBy(() -> assembler.assemble(id)).isInstanceOf(QuoteNotFoundException.class);
    }

    @ParameterizedTest
    @EnumSource(QuoteStatus.class)
    void assemble_producesNonNullBannerForEveryStatus(QuoteStatus status) {
        when(quoteApi.requireById(new QuoteId(QUOTE_ULID)))
                .thenReturn(quoteSummary(status, new BigDecimal("1800")));
        stubLabSettings();

        var model = assembler.assemble(new QuoteId(QUOTE_ULID));

        assertThat(model.statusBanner()).isNotNull();
        assertThat(model.statusBanner().title()).isNotBlank();
    }

    // ----- helpers --------------------------------------------------------------------------

    private void stubLabSettings() {
        when(labSettingsApi.get())
                .thenReturn(
                        new LabSettings(
                                "Lab Sparkcorp",
                                "12 Avenue du Test, Lomé",
                                "ACC-2026-001",
                                "Dr. Manager",
                                Instant.parse("2026-05-26T00:00:00Z")));
    }

    private QuoteSummary quoteSummary(QuoteStatus status, BigDecimal totalAmount) {
        return new QuoteSummary(
                new QuoteId(QUOTE_ULID),
                new QuoteNumber(QUOTE_NUMBER_VALUE),
                new LabFileSummary.PatientSnapshot(
                        PATIENT_ULID, "PAT-260526-0001", "DOE", "Jane", Sex.F),
                new LabFileSummary.PrescriberSnapshot(
                        PRESCRIBER_ULID, "PRX-001", "Dr. KOFFI", "GEN"),
                status,
                status == QuoteStatus.DRAFT ? null : Instant.parse("2026-05-26T09:00:00Z"),
                status == QuoteStatus.CONVERTED ? LAB_FILE_ULID : null,
                status == QuoteStatus.CONVERTED ? Instant.parse("2026-05-26T10:00:00Z") : null,
                Instant.parse("2026-06-25T00:00:00Z"),
                totalAmount,
                List.of(
                        new QuoteSummary.QuoteLineSummary(
                                new QuoteLineId(QUOTE_LINE_ULID),
                                ANALYSIS_ULID,
                                "GLU",
                                new BigDecimal("450"),
                                new BigDecimal("450"),
                                1,
                                totalAmount)),
                Instant.parse("2026-05-26T08:00:00Z"),
                USER_ULID);
    }

    private QuoteSummary quoteSummaryWithoutPrescriber() {
        var base = quoteSummary(QuoteStatus.LOCKED, new BigDecimal("1800"));
        return new QuoteSummary(
                base.id(),
                base.quoteNumber(),
                base.patient(),
                null, // prescriber
                base.status(),
                base.lockedAt(),
                base.labFileId(),
                base.convertedAt(),
                base.expiresAt(),
                base.totalAmount(),
                base.lines(),
                base.createdAt(),
                base.createdByUserId());
    }
}
