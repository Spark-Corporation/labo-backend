package org.sparkcorporation.lims.order.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.sparkcorporation.lims.notification.api.Channel;
import org.sparkcorporation.lims.notification.api.ContextType;
import org.sparkcorporation.lims.notification.api.EmailRequest;
import org.sparkcorporation.lims.notification.api.NotificationId;
import org.sparkcorporation.lims.notification.api.NotificationRequest;
import org.sparkcorporation.lims.notification.api.NotificationService;
import org.sparkcorporation.lims.notification.api.WhatsAppRequest;
import org.sparkcorporation.lims.order.api.LabFileSummary.PatientSnapshot;
import org.sparkcorporation.lims.order.api.QuoteApi;
import org.sparkcorporation.lims.order.api.QuoteId;
import org.sparkcorporation.lims.order.api.QuoteNumber;
import org.sparkcorporation.lims.order.api.QuoteStatus;
import org.sparkcorporation.lims.order.api.QuoteSummary;
import org.sparkcorporation.lims.order.domain.MissingRecipientContactException;
import org.sparkcorporation.lims.order.domain.QuoteNotSendableException;
import org.sparkcorporation.lims.order.infra.QuoteSendService;
import org.sparkcorporation.lims.order.infra.pdf.QuoteReportModelAssembler;
import org.sparkcorporation.lims.order.infra.pdf.QuoteReportPdfDocument;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.RecipientContact;
import org.sparkcorporation.lims.pdf.api.PdfRenderingService;
import org.sparkcorporation.lims.pdf.api.RenderedPdf;
import org.sparkcorporation.lims.shared.domain.Sex;

@ExtendWith(MockitoExtension.class)
class QuoteSendServiceTest {

    private static final String QUOTE_ULID = "01HZB0000000000000000QT0TE";
    private static final String PATIENT_ULID = "01HZB0000000000000000PAT1N";
    private static final Instant FIXED_NOW = Instant.parse("2026-05-29T09:00:00Z");

    @Mock private QuoteApi quoteApi;
    @Mock private QuoteReportModelAssembler assembler;
    @Mock private QuoteReportPdfDocument docType;
    @Mock private PdfRenderingService pdfService;
    @Mock private PatientApi patientApi;
    @Mock private NotificationService notificationService;

    private QuoteSendService service;

    @BeforeEach
    void setUp() {
        service =
                new QuoteSendService(
                        quoteApi, assembler, docType, pdfService, patientApi, notificationService);
    }

    @Test
    void emailChannel_rendersPdf_resolvesEmail_andSendsEmailRequestWithAttachment() {
        when(quoteApi.requireById(new QuoteId(QUOTE_ULID))).thenReturn(quote(QuoteStatus.LOCKED));
        when(assembler.assemble(new QuoteId(QUOTE_ULID))).thenReturn(null);
        when(pdfService.render(any(), any())).thenReturn(renderedPdf());
        when(patientApi.contactFor(new PatientId(PATIENT_ULID)))
                .thenReturn(new RecipientContact("jane@example.test", "+22890000000", null));
        var generated = NotificationId.next();
        when(notificationService.send(any(NotificationRequest.class))).thenReturn(generated);

        var ids = service.send(new QuoteId(QUOTE_ULID), Set.of(Channel.EMAIL));

        assertThat(ids).containsExactly(generated);
        var captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).send(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(EmailRequest.class);
        var email = (EmailRequest) captor.getValue();
        assertThat(email.recipient()).isEqualTo("jane@example.test");
        assertThat(email.subject()).isNotBlank();
        assertThat(email.attachment()).isNotNull();
        assertThat(email.attachment().contentType()).isEqualTo("application/pdf");
        assertThat(email.attachment().bytes()).isEqualTo(new byte[] {'%', 'P', 'D', 'F'});
        assertThat(email.context().type()).isEqualTo(ContextType.QUOTE);
        assertThat(email.context().id()).isEqualTo(QUOTE_ULID);
    }

    @Test
    void whatsAppChannel_usesPhone_andSendsWhatsAppRequest() {
        when(quoteApi.requireById(new QuoteId(QUOTE_ULID)))
                .thenReturn(quote(QuoteStatus.CONVERTED));
        when(patientApi.contactFor(new PatientId(PATIENT_ULID)))
                .thenReturn(new RecipientContact(null, "+22890000000", null));
        when(notificationService.send(any(NotificationRequest.class)))
                .thenReturn(NotificationId.next());

        service.send(new QuoteId(QUOTE_ULID), Set.of(Channel.WHATSAPP));

        var captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).send(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(WhatsAppRequest.class);
        var wa = (WhatsAppRequest) captor.getValue();
        assertThat(wa.recipient()).isEqualTo("+22890000000");
        assertThat(wa.body()).isNotBlank();
        assertThat(wa.context().type()).isEqualTo(ContextType.QUOTE);
    }

    @Test
    void whatsAppChannel_fallsBackToGuardianPhone_whenPatientHasNoPhone() {
        when(quoteApi.requireById(new QuoteId(QUOTE_ULID))).thenReturn(quote(QuoteStatus.LOCKED));
        when(patientApi.contactFor(new PatientId(PATIENT_ULID)))
                .thenReturn(new RecipientContact(null, null, "+22899999999"));
        when(notificationService.send(any(NotificationRequest.class)))
                .thenReturn(NotificationId.next());

        service.send(new QuoteId(QUOTE_ULID), Set.of(Channel.WHATSAPP));

        var captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).send(captor.capture());
        assertThat(((WhatsAppRequest) captor.getValue()).recipient()).isEqualTo("+22899999999");
    }

    @Test
    void draftQuote_throwsQuoteNotSendable_andSendsNothing() {
        when(quoteApi.requireById(new QuoteId(QUOTE_ULID))).thenReturn(quote(QuoteStatus.DRAFT));

        assertThatThrownBy(() -> service.send(new QuoteId(QUOTE_ULID), Set.of(Channel.EMAIL)))
                .isInstanceOf(QuoteNotSendableException.class);
        verify(notificationService, never()).send(any());
    }

    @Test
    void expiredQuote_throwsQuoteNotSendable() {
        when(quoteApi.requireById(new QuoteId(QUOTE_ULID))).thenReturn(quote(QuoteStatus.EXPIRED));

        assertThatThrownBy(() -> service.send(new QuoteId(QUOTE_ULID), Set.of(Channel.EMAIL)))
                .isInstanceOf(QuoteNotSendableException.class);
    }

    @Test
    void emailRequested_butNoEmailOnFile_throwsMissingRecipientContact() {
        when(quoteApi.requireById(new QuoteId(QUOTE_ULID))).thenReturn(quote(QuoteStatus.LOCKED));
        when(patientApi.contactFor(new PatientId(PATIENT_ULID)))
                .thenReturn(new RecipientContact(null, "+22890000000", null));

        assertThatThrownBy(() -> service.send(new QuoteId(QUOTE_ULID), Set.of(Channel.EMAIL)))
                .isInstanceOf(MissingRecipientContactException.class);
        verify(notificationService, never()).send(any());
        // Spec §5.1: the 422 short-circuits before the PDF render.
        verify(pdfService, never()).render(any(), any());
    }

    @Test
    void whatsAppRequested_butNoPhoneNorGuardian_throwsMissingRecipientContact() {
        when(quoteApi.requireById(new QuoteId(QUOTE_ULID))).thenReturn(quote(QuoteStatus.LOCKED));
        when(patientApi.contactFor(new PatientId(PATIENT_ULID)))
                .thenReturn(new RecipientContact("jane@example.test", null, null));

        assertThatThrownBy(() -> service.send(new QuoteId(QUOTE_ULID), Set.of(Channel.WHATSAPP)))
                .isInstanceOf(MissingRecipientContactException.class);
        verify(notificationService, never()).send(any());
    }

    private static RenderedPdf renderedPdf() {
        return new RenderedPdf(new byte[] {'%', 'P', 'D', 'F'}, "devis.pdf", FIXED_NOW);
    }

    private static QuoteSummary quote(QuoteStatus status) {
        return new QuoteSummary(
                new QuoteId(QUOTE_ULID),
                new QuoteNumber("DEV-260529-0001"),
                new PatientSnapshot(PATIENT_ULID, "P001", "Doe", "Jane", Sex.F),
                null,
                status,
                null,
                null,
                null,
                null,
                BigDecimal.ZERO,
                List.of(),
                FIXED_NOW,
                null);
    }
}
