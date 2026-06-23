package org.sparkcorporation.lims.order.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisCode;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary;
import org.sparkcorporation.lims.catalog.api.DisciplineId;
import org.sparkcorporation.lims.catalog.api.PrescriberApi;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.LabFileSummary.PatientSnapshot;
import org.sparkcorporation.lims.order.api.QuoteApi;
import org.sparkcorporation.lims.order.api.QuoteId;
import org.sparkcorporation.lims.order.api.QuoteNumber;
import org.sparkcorporation.lims.order.api.QuoteStatus;
import org.sparkcorporation.lims.order.api.QuoteSummary;
import org.sparkcorporation.lims.order.domain.DuplicateAnalysisRequestException;
import org.sparkcorporation.lims.order.domain.OrderInvalidAnalysisReferenceException;
import org.sparkcorporation.lims.order.domain.OrderInvalidPatientReferenceException;
import org.sparkcorporation.lims.order.domain.Quote;
import org.sparkcorporation.lims.order.domain.QuoteAlreadyConvertedException;
import org.sparkcorporation.lims.order.domain.QuoteNotEditableException;
import org.sparkcorporation.lims.order.domain.QuoteNotFoundException;
import org.sparkcorporation.lims.order.infra.DefaultQuoteNumberGenerator;
import org.sparkcorporation.lims.order.infra.QuoteRepository;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;
import org.sparkcorporation.lims.shared.domain.Sex;
import org.sparkcorporation.lims.shared.money.PricingProperties;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

    private static final String VALID_ULID = "01HZB0000000000000000ADMNZ";
    private static final Instant FIXED_NOW = Instant.parse("2026-05-08T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private static final BigDecimal B_RATE = new BigDecimal("400");

    @Mock private QuoteRepository quoteRepo;
    @Mock private QuoteApi quoteApi;
    @Mock private PatientApi patientApi;
    @Mock private PrescriberApi prescriberApi;
    @Mock private AnalysisApi analysisApi;
    @Mock private DefaultQuoteNumberGenerator quoteNumberGen;
    @Mock private LabFileService labFileService;

    private final PricingProperties pricing = new PricingProperties(B_RATE);

    private QuoteService service;

    @BeforeEach
    void setUp() {
        service =
                new QuoteService(
                        quoteRepo,
                        quoteApi,
                        patientApi,
                        prescriberApi,
                        analysisApi,
                        quoteNumberGen,
                        labFileService,
                        pricing,
                        FIXED_CLOCK);
    }

    private static AnalysisSummary analysisSummary(String code) {
        return new AnalysisSummary(
                AnalysisId.generate(),
                code,
                "Glucose",
                AnalysisKind.SIMPLE,
                BigDecimal.valueOf(20),
                1,
                new AnalysisSummary.DisciplineSnapshot(
                        DisciplineId.next(), "BIO", "Biochimie", DisciplinePrefix.BIOCHIMIE),
                null,
                null,
                ResultType.REAL,
                null,
                null,
                null,
                new AnalysisSummary.UnitsSummary(null, null, null, null, null),
                new AnalysisSummary.AnalyticalDelaySummary(null, null, null),
                List.of(),
                List.of());
    }

    private static QuoteSummary quoteSummary() {
        return new QuoteSummary(
                new QuoteId(VALID_ULID),
                new QuoteNumber("DEV-260508-0001"),
                new PatientSnapshot(VALID_ULID, "P001", "Doe", "Jane", Sex.F),
                null,
                QuoteStatus.DRAFT,
                null,
                null,
                null,
                null,
                BigDecimal.ZERO,
                List.of(),
                FIXED_NOW,
                null);
    }

    @Test
    void create_validates_patient_first_then_throws_on_unknown_patient() {
        when(patientApi.existsById(any(PatientId.class))).thenReturn(false);

        var req = new CreateQuoteRequest(VALID_ULID, null, List.of("GLU"), null);

        assertThatThrownBy(() -> service.create(req, "user-1"))
                .isInstanceOf(OrderInvalidPatientReferenceException.class);
        // counter must NOT be bumped — sequence number must not be burned
        verify(quoteNumberGen, never()).next(any());
    }

    @Test
    void create_throws_on_unknown_analysis_code_without_burning_counter() {
        when(patientApi.existsById(any(PatientId.class))).thenReturn(true);
        when(analysisApi.findByCode(any(AnalysisCode.class))).thenReturn(Optional.empty());

        var req = new CreateQuoteRequest(VALID_ULID, null, List.of("UNKNOWN"), null);

        assertThatThrownBy(() -> service.create(req, "user-1"))
                .isInstanceOf(OrderInvalidAnalysisReferenceException.class);
        verify(quoteNumberGen, never()).next(any());
    }

    @Test
    void create_throws_on_duplicate_analysis_in_request_without_burning_counter() {
        when(patientApi.existsById(any(PatientId.class))).thenReturn(true);
        when(analysisApi.findByCode(new AnalysisCode("GLU")))
                .thenReturn(Optional.of(analysisSummary("GLU")));

        var req = new CreateQuoteRequest(VALID_ULID, null, List.of("GLU", "GLU"), null);

        assertThatThrownBy(() -> service.create(req, "user-1"))
                .isInstanceOf(DuplicateAnalysisRequestException.class);
        verify(quoteNumberGen, never()).next(any());
    }

    @Test
    void create_succeeds_and_bumps_counter_after_all_validation_passes() {
        when(patientApi.existsById(any(PatientId.class))).thenReturn(true);
        when(analysisApi.findByCode(new AnalysisCode("GLU")))
                .thenReturn(Optional.of(analysisSummary("GLU")));
        when(quoteNumberGen.next(LocalDate.of(2026, 5, 8)))
                .thenReturn(new QuoteNumber("DEV-260508-0001"));
        when(quoteApi.requireById(any(QuoteId.class))).thenReturn(quoteSummary());

        var req = new CreateQuoteRequest(VALID_ULID, null, List.of("GLU"), null);

        var resp = service.create(req, "user-1");

        assertThat(resp.quoteNumber()).isEqualTo("DEV-260508-0001");
        assertThat(resp.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(quoteNumberGen, times(1)).next(LocalDate.of(2026, 5, 8));
        verify(quoteRepo, times(1)).save(any());
    }

    @Test
    void lock_reads_pricing_bRate_and_passes_to_domain() {
        // Arrange a DRAFT quote with one line so :lock runs end-to-end
        var quote =
                Quote.draft(
                        new org.sparkcorporation.lims.order.domain.NewQuote(
                                VALID_ULID, null, null, List.of()),
                        new QuoteNumber("DEV-260508-0002"),
                        null,
                        FIXED_CLOCK);
        quote.addLine(
                new org.sparkcorporation.lims.order.domain.NewQuoteLine(
                        AnalysisId.generate().value(), "GLU", new BigDecimal("10.00"), 1),
                FIXED_CLOCK);

        when(quoteRepo.findActiveById(quote.getId())).thenReturn(Optional.of(quote));
        when(quoteApi.requireById(any())).thenReturn(quoteSummary());

        service.lock(quote.quoteId());

        // post-lock the line carries the configured bRate
        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.LOCKED);
        assertThat(quote.getLines().get(0).getBRateSnapshot()).isEqualByComparingTo(B_RATE);
        // 10.00 × 400 × 1 = 4000.00
        assertThat(quote.getLines().get(0).getLineTotal())
                .isEqualByComparingTo(new BigDecimal("4000.00"));
        assertThat(quote.getTotalAmount()).isEqualByComparingTo(new BigDecimal("4000.00"));
    }

    @Test
    void convert_calls_labFileService_create_with_quote_data() {
        var analysisId = AnalysisId.generate().value();
        var quote =
                Quote.draft(
                        new org.sparkcorporation.lims.order.domain.NewQuote(
                                VALID_ULID, null, null, List.of()),
                        new QuoteNumber("DEV-260508-0003"),
                        null,
                        FIXED_CLOCK);
        quote.addLine(
                new org.sparkcorporation.lims.order.domain.NewQuoteLine(
                        analysisId, "GLU", new BigDecimal("10.00"), 1),
                FIXED_CLOCK);
        quote.lock(B_RATE, FIXED_CLOCK);

        when(quoteRepo.findActiveById(quote.getId())).thenReturn(Optional.of(quote));
        var labFileResponse =
                new LabFileResponse(
                        VALID_ULID,
                        "LF-260508-0001",
                        new LabFileResponse.PatientSnapshotResponse(
                                VALID_ULID, "P001", "Doe", null, Sex.F),
                        null,
                        null,
                        false,
                        org.sparkcorporation.lims.order.api.LabFileStatus.OPEN,
                        List.of(),
                        List.of(),
                        FIXED_NOW,
                        null,
                        null);
        when(labFileService.create(any(CreateLabFileRequest.class), eq("user-1")))
                .thenReturn(labFileResponse);

        var resp = service.convert(quote.quoteId(), new ConvertQuoteRequest(true), "user-1");

        var captor = ArgumentCaptor.forClass(CreateLabFileRequest.class);
        verify(labFileService, times(1)).create(captor.capture(), eq("user-1"));
        var passed = captor.getValue();
        assertThat(passed.patientId()).isEqualTo(VALID_ULID);
        assertThat(passed.urgent()).isTrue();
        assertThat(passed.preleveurId()).isNull();
        assertThat(passed.analyses()).containsExactly("GLU");

        assertThat(resp.id()).isEqualTo(VALID_ULID);
        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.CONVERTED);
        assertThat(quote.getLabFileId()).isEqualTo(VALID_ULID);
    }

    @Test
    void convert_throws_quoteAlreadyConverted_when_already_converted() {
        var analysisId = AnalysisId.generate().value();
        var quote =
                Quote.draft(
                        new org.sparkcorporation.lims.order.domain.NewQuote(
                                VALID_ULID, null, null, List.of()),
                        new QuoteNumber("DEV-260508-0004"),
                        null,
                        FIXED_CLOCK);
        quote.addLine(
                new org.sparkcorporation.lims.order.domain.NewQuoteLine(
                        analysisId, "GLU", new BigDecimal("10.00"), 1),
                FIXED_CLOCK);
        quote.lock(B_RATE, FIXED_CLOCK);
        quote.convert(new org.sparkcorporation.lims.order.api.LabFileId(VALID_ULID), FIXED_CLOCK);

        when(quoteRepo.findActiveById(quote.getId())).thenReturn(Optional.of(quote));

        assertThatThrownBy(
                        () ->
                                service.convert(
                                        quote.quoteId(), new ConvertQuoteRequest(false), "user-1"))
                .isInstanceOf(QuoteAlreadyConvertedException.class);
        // labFileService NOT called — failure short-circuits before lab-file creation
        verify(labFileService, never()).create(any(), any());
    }

    @Test
    void convert_throws_quoteNotEditable_when_DRAFT() {
        var quote =
                Quote.draft(
                        new org.sparkcorporation.lims.order.domain.NewQuote(
                                VALID_ULID, null, null, List.of()),
                        new QuoteNumber("DEV-260508-0005"),
                        null,
                        FIXED_CLOCK);

        when(quoteRepo.findActiveById(quote.getId())).thenReturn(Optional.of(quote));

        assertThatThrownBy(
                        () ->
                                service.convert(
                                        quote.quoteId(), new ConvertQuoteRequest(false), "user-1"))
                .isInstanceOf(QuoteNotEditableException.class);
        verify(labFileService, never()).create(any(), any());
    }

    @Test
    void delete_is_idempotent_when_already_inactive() {
        when(quoteRepo.findActiveById(VALID_ULID)).thenReturn(Optional.empty());
        service.delete(new QuoteId(VALID_ULID));
        verify(quoteRepo, never()).delete(any());
    }

    @Test
    void update_throws_when_quote_not_found() {
        when(quoteRepo.findActiveById(VALID_ULID)).thenReturn(Optional.empty());
        assertThatThrownBy(
                        () ->
                                service.update(
                                        new QuoteId(VALID_ULID),
                                        new org.sparkcorporation.lims.order.domain.UpdateQuote(
                                                null, null)))
                .isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    void create_throws_on_unknown_prescriber_reference() {
        var prescriberUlid = org.sparkcorporation.lims.shared.id.Ulid.generate();
        when(patientApi.existsById(any(PatientId.class))).thenReturn(true);
        when(prescriberApi.existsById(any())).thenReturn(false);

        var req = new CreateQuoteRequest(VALID_ULID, prescriberUlid, List.of("GLU"), null);

        assertThatThrownBy(() -> service.create(req, "user-1"))
                .isInstanceOf(
                        org.sparkcorporation.lims.order.domain
                                .OrderInvalidPrescriberReferenceException.class);
        verify(quoteNumberGen, never()).next(any());
    }

    @Test
    void delete_when_present_calls_repository_delete() {
        var quote =
                Quote.draft(
                        new org.sparkcorporation.lims.order.domain.NewQuote(
                                VALID_ULID, null, null, List.of()),
                        new QuoteNumber("DEV-260508-0099"),
                        null,
                        FIXED_CLOCK);
        when(quoteRepo.findActiveById(quote.getId())).thenReturn(Optional.of(quote));

        service.delete(quote.quoteId());

        verify(quoteRepo, times(1)).delete(quote);
    }

    @Test
    void addLines_throws_when_quote_missing() {
        when(quoteRepo.findActiveById(VALID_ULID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addLines(new QuoteId(VALID_ULID), List.of("GLU")))
                .isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    void addLines_throws_on_duplicate_code_in_request() {
        var quote =
                Quote.draft(
                        new org.sparkcorporation.lims.order.domain.NewQuote(
                                VALID_ULID, null, null, List.of()),
                        new QuoteNumber("DEV-260508-0010"),
                        null,
                        FIXED_CLOCK);
        when(quoteRepo.findActiveById(quote.getId())).thenReturn(Optional.of(quote));
        when(analysisApi.findByCode(new AnalysisCode("GLU")))
                .thenReturn(Optional.of(analysisSummary("GLU")));

        assertThatThrownBy(() -> service.addLines(quote.quoteId(), List.of("GLU", "GLU")))
                .isInstanceOf(DuplicateAnalysisRequestException.class);
    }

    @Test
    void addLines_throws_on_unknown_analysis_code() {
        var quote =
                Quote.draft(
                        new org.sparkcorporation.lims.order.domain.NewQuote(
                                VALID_ULID, null, null, List.of()),
                        new QuoteNumber("DEV-260508-0011"),
                        null,
                        FIXED_CLOCK);
        when(quoteRepo.findActiveById(quote.getId())).thenReturn(Optional.of(quote));
        when(analysisApi.findByCode(any(AnalysisCode.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addLines(quote.quoteId(), List.of("MISSING")))
                .isInstanceOf(OrderInvalidAnalysisReferenceException.class);
    }

    @Test
    void removeLine_throws_when_quote_missing() {
        when(quoteRepo.findActiveById(VALID_ULID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.removeLine(
                                        new QuoteId(VALID_ULID),
                                        org.sparkcorporation.lims.order.api.QuoteLineId.next()))
                .isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    void lock_throws_when_quote_missing() {
        when(quoteRepo.findActiveById(VALID_ULID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.lock(new QuoteId(VALID_ULID)))
                .isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    void convert_throws_when_quote_missing() {
        when(quoteRepo.findActiveById(VALID_ULID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.convert(
                                        new QuoteId(VALID_ULID),
                                        new ConvertQuoteRequest(false),
                                        "user-1"))
                .isInstanceOf(QuoteNotFoundException.class);
    }
}
