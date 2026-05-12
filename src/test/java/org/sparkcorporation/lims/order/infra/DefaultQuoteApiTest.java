package org.sparkcorporation.lims.order.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.PrescriberApi;
import org.sparkcorporation.lims.catalog.api.PrescriberId;
import org.sparkcorporation.lims.catalog.api.PrescriberSummary;
import org.sparkcorporation.lims.catalog.api.SpecialtyId;
import org.sparkcorporation.lims.order.api.QuoteId;
import org.sparkcorporation.lims.order.api.QuoteNumber;
import org.sparkcorporation.lims.order.domain.NewQuote;
import org.sparkcorporation.lims.order.domain.NewQuoteLine;
import org.sparkcorporation.lims.order.domain.Quote;
import org.sparkcorporation.lims.order.domain.QuoteNotFoundException;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientSummary;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;
import org.sparkcorporation.lims.shared.id.Ulid;

class DefaultQuoteApiTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-08T08:00:00Z"), ZoneOffset.UTC);

    private final QuoteRepository quoteRepo = mock(QuoteRepository.class);
    private final QuoteLineRepository lineRepo = mock(QuoteLineRepository.class);
    private final PatientApi patientApi = mock(PatientApi.class);
    private final PrescriberApi prescriberApi = mock(PrescriberApi.class);

    private final DefaultQuoteApi api =
            new DefaultQuoteApi(quoteRepo, lineRepo, patientApi, prescriberApi);

    private final String patientId = Ulid.generate();
    private final String prescriberId = Ulid.generate();
    private final String analysisId = Ulid.generate();

    private Quote freshQuote() {
        var cmd = new NewQuote(patientId, prescriberId, null, List.of());
        return Quote.draft(cmd, new QuoteNumber("DEV-260508-0001"), null, FIXED);
    }

    private Quote freshQuoteWithoutPrescriber() {
        var cmd = new NewQuote(patientId, null, null, List.of());
        return Quote.draft(cmd, new QuoteNumber("DEV-260508-0002"), null, FIXED);
    }

    private PatientSummary patientSummary() {
        return new PatientSummary(
                new PatientId(patientId),
                "LAB-260508-001",
                "DOE",
                "Jane",
                Civility.MADAME,
                Sex.F,
                null,
                false,
                false,
                false);
    }

    private PrescriberSummary prescriberSummary() {
        return new PrescriberSummary(
                new PrescriberId(prescriberId),
                "DR-001",
                "Dr",
                "Martin",
                new PrescriberSummary.SpecialtySnapshot(
                        new SpecialtyId(Ulid.generate()), "CARD", "Cardiology"),
                null,
                null,
                null,
                null);
    }

    @Test
    void findById_returns_summary_with_patient_and_prescriber_snapshots() {
        var quote = freshQuote();
        quote.addLine(new NewQuoteLine(analysisId, "GLUC", new BigDecimal("10.00"), 1), FIXED);

        when(quoteRepo.findActiveById(quote.getId())).thenReturn(Optional.of(quote));
        when(lineRepo.findByQuoteId(quote.getId())).thenReturn(quote.linesView());
        when(patientApi.findById(any())).thenReturn(Optional.of(patientSummary()));
        when(prescriberApi.findById(any())).thenReturn(Optional.of(prescriberSummary()));

        var summary = api.findById(quote.quoteId()).orElseThrow();

        assertThat(summary.patient().id()).isEqualTo(patientId);
        assertThat(summary.patient().lastName()).isEqualTo("DOE");
        assertThat(summary.prescriber()).isNotNull();
        assertThat(summary.prescriber().code()).isEqualTo("DR-001");
        assertThat(summary.prescriber().specialtyCode()).isEqualTo("CARD");
        assertThat(summary.lines()).hasSize(1);
        assertThat(summary.lines().get(0).analysisCode()).isEqualTo("GLUC");
    }

    @Test
    void findById_returns_empty_when_quote_missing() {
        when(quoteRepo.findActiveById(any())).thenReturn(Optional.empty());
        assertThat(api.findById(QuoteId.next())).isEmpty();
    }

    @Test
    void findById_renders_null_prescriber_when_id_is_null() {
        var quote = freshQuoteWithoutPrescriber();

        when(quoteRepo.findActiveById(quote.getId())).thenReturn(Optional.of(quote));
        when(lineRepo.findByQuoteId(quote.getId())).thenReturn(List.of());
        when(patientApi.findById(any())).thenReturn(Optional.of(patientSummary()));

        var summary = api.findById(quote.quoteId()).orElseThrow();

        assertThat(summary.prescriber()).isNull();
    }

    @Test
    void findById_renders_null_prescriber_when_catalog_returns_empty_soft_deleted() {
        var quote = freshQuote();

        when(quoteRepo.findActiveById(quote.getId())).thenReturn(Optional.of(quote));
        when(lineRepo.findByQuoteId(quote.getId())).thenReturn(List.of());
        when(patientApi.findById(any())).thenReturn(Optional.of(patientSummary()));
        when(prescriberApi.findById(any())).thenReturn(Optional.empty()); // soft-deleted

        var summary = api.findById(quote.quoteId()).orElseThrow();

        assertThat(summary.prescriber()).isNull();
    }

    @Test
    void findById_throws_when_patient_invariant_violated_unknown_patient() {
        var quote = freshQuote();

        when(quoteRepo.findActiveById(quote.getId())).thenReturn(Optional.of(quote));
        when(lineRepo.findByQuoteId(quote.getId())).thenReturn(List.of());
        // Patient FK is NOT NULL + @SQLRestriction; missing row is an invariant violation.
        when(patientApi.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> api.findById(quote.quoteId()))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void requireById_throws_QuoteNotFoundException_when_missing() {
        when(quoteRepo.findActiveById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> api.requireById(QuoteId.next()))
                .isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    void existsById_returns_true_when_present() {
        var quote = freshQuote();
        when(quoteRepo.findActiveById(quote.getId())).thenReturn(Optional.of(quote));
        assertThat(api.existsById(quote.quoteId())).isTrue();
    }

    @Test
    void existsById_returns_false_when_lookup_empty() {
        when(quoteRepo.findActiveById(any())).thenReturn(Optional.empty());
        assertThat(api.existsById(QuoteId.next())).isFalse();
    }

    @Test
    void findByQuoteNumber_returns_summary_when_present() {
        var quote = freshQuote();
        when(quoteRepo.findActiveByQuoteNumber("DEV-260508-0001")).thenReturn(Optional.of(quote));
        when(lineRepo.findByQuoteId(quote.getId())).thenReturn(List.of());
        when(patientApi.findById(any())).thenReturn(Optional.of(patientSummary()));
        when(prescriberApi.findById(any())).thenReturn(Optional.of(prescriberSummary()));

        var summary = api.findByQuoteNumber(new QuoteNumber("DEV-260508-0001")).orElseThrow();

        assertThat(summary.quoteNumber().value()).isEqualTo("DEV-260508-0001");
    }

    @Test
    void findActiveByPatient_returns_summaries_for_each_active_quote() {
        var q1 = freshQuote();
        when(quoteRepo.findActiveByPatientId(patientId)).thenReturn(List.of(q1));
        when(lineRepo.findByQuoteId(q1.getId())).thenReturn(List.of());
        when(patientApi.findById(any())).thenReturn(Optional.of(patientSummary()));
        when(prescriberApi.findById(any())).thenReturn(Optional.of(prescriberSummary()));

        var summaries = api.findActiveByPatient(patientId);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).id().value()).isEqualTo(q1.getId());
    }
}
