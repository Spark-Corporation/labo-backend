package org.sparkcorporation.lims.delivery.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import dev.sparksam.securedx.core.signing.ActionSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.sparkcorporation.lims.delivery.domain.MissingPatientEmailException;
import org.sparkcorporation.lims.delivery.domain.NotSendableException;
import org.sparkcorporation.lims.notification.api.ContextType;
import org.sparkcorporation.lims.notification.api.EmailRequest;
import org.sparkcorporation.lims.notification.api.NotificationId;
import org.sparkcorporation.lims.notification.api.NotificationRequest;
import org.sparkcorporation.lims.notification.api.NotificationService;
import org.sparkcorporation.lims.notification.api.WhatsAppRequest;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.order.api.LabFileSummary.BillingBlock;
import org.sparkcorporation.lims.order.api.LabFileSummary.PatientSnapshot;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.RecipientContact;
import org.sparkcorporation.lims.result.api.CompteRenduApi;
import org.sparkcorporation.lims.result.api.CompteRenduPdf;
import org.sparkcorporation.lims.shared.domain.Sex;

@ExtendWith(MockitoExtension.class)
class SendDossierServiceTest {

    private static final String LAB_FILE_ULID = "01KST62KS753K8N7HKH9JH69W8";
    private static final String PATIENT_ULID = "01KST62KS7YE32R60RT68ST2GY";
    private static final String EMAIL_NOTIF_ULID = "01KST62KS7348J09SX0M8TVDK3";
    private static final String WA_NOTIF_ULID = "01KST62KS7S2J86N8A5NVRG4NC";
    private static final String SENT_BY = "01KST62KS753K8N7HKH9JH699Z";
    private static final byte[] PDF_BYTES = {'%', 'P', 'D', 'F'};
    private static final byte[] ENCRYPTED_BYTES = {'E', 'N', 'C'};

    @Mock private LabFileApi labFileApi;
    @Mock private CompteRenduApi compteRenduApi;
    @Mock private PdfPasswordProtector pdfPasswordProtector;
    @Mock private PatientApi patientApi;
    @Mock private NotificationService notificationService;
    @Mock private ActionSigner actionSigner;

    private SendDossierService service;

    @BeforeEach
    void setUp() {
        service =
                new SendDossierService(
                        labFileApi,
                        compteRenduApi,
                        pdfPasswordProtector,
                        patientApi,
                        notificationService,
                        actionSigner);
    }

    @Test
    void
            published_withEmailAndPhone_encryptsAndEmails_andSendsPasswordOverWhatsApp_noPasswordReturned() {
        stubPublishedDossier();
        when(patientApi.contactFor(new PatientId(PATIENT_ULID)))
                .thenReturn(new RecipientContact("jane@example.test", "+22890000000", null));
        when(notificationService.send(any(EmailRequest.class)))
                .thenReturn(new NotificationId(EMAIL_NOTIF_ULID));
        when(notificationService.send(any(WhatsAppRequest.class)))
                .thenReturn(new NotificationId(WA_NOTIF_ULID));

        var result = service.send(new LabFileId(LAB_FILE_ULID), SENT_BY);

        assertThat(result.emailId()).isEqualTo(new NotificationId(EMAIL_NOTIF_ULID));
        assertThat(result.whatsAppId()).isEqualTo(new NotificationId(WA_NOTIF_ULID));
        assertThat(result.password()).isNull();

        var captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService, org.mockito.Mockito.times(2)).send(captor.capture());
        var requests = captor.getAllValues();
        var email =
                (EmailRequest)
                        requests.stream().filter(r -> r instanceof EmailRequest).findFirst().get();
        assertThat(email.recipient()).isEqualTo("jane@example.test");
        assertThat(email.attachment()).isNotNull();
        assertThat(email.attachment().bytes()).isEqualTo(ENCRYPTED_BYTES);
        assertThat(email.attachment().contentType()).isEqualTo("application/pdf");
        assertThat(email.context().type()).isEqualTo(ContextType.LAB_FILE);
        assertThat(email.context().id()).isEqualTo(LAB_FILE_ULID);
        var wa =
                (WhatsAppRequest)
                        requests.stream()
                                .filter(r -> r instanceof WhatsAppRequest)
                                .findFirst()
                                .get();
        assertThat(wa.recipient()).isEqualTo("+22890000000");
        assertThat(wa.context().type()).isEqualTo(ContextType.LAB_FILE);
    }

    @Test
    void recordsSignedAction_withRawEmailNotificationUlidString() {
        stubPublishedDossier();
        when(patientApi.contactFor(new PatientId(PATIENT_ULID)))
                .thenReturn(new RecipientContact("jane@example.test", "+22890000000", null));
        when(notificationService.send(any(EmailRequest.class)))
                .thenReturn(new NotificationId(EMAIL_NOTIF_ULID));
        when(notificationService.send(any(WhatsAppRequest.class)))
                .thenReturn(new NotificationId(WA_NOTIF_ULID));

        service.send(new LabFileId(LAB_FILE_ULID), SENT_BY);

        verify(actionSigner)
                .sign(
                        eq("dossier.send"),
                        eq(LAB_FILE_ULID),
                        eq(Map.of("channel", "EMAIL", "notificationId", EMAIL_NOTIF_ULID)));
    }

    @Test
    void published_withNoPhone_returnsPassword_andSendsOnlyEmail() {
        stubPublishedDossier();
        when(patientApi.contactFor(new PatientId(PATIENT_ULID)))
                .thenReturn(new RecipientContact("jane@example.test", null, null));
        when(notificationService.send(any(EmailRequest.class)))
                .thenReturn(new NotificationId(EMAIL_NOTIF_ULID));

        var result = service.send(new LabFileId(LAB_FILE_ULID), SENT_BY);

        assertThat(result.emailId()).isEqualTo(new NotificationId(EMAIL_NOTIF_ULID));
        assertThat(result.whatsAppId()).isNull();
        assertThat(result.password()).isNotBlank();
        verify(notificationService, never()).send(any(WhatsAppRequest.class));
    }

    @Test
    void notPublished_throwsNotSendable_andSendsNothing() {
        when(labFileApi.requireById(new LabFileId(LAB_FILE_ULID)))
                .thenReturn(summary(LabFileStatus.SIGNED));

        assertThatThrownBy(() -> service.send(new LabFileId(LAB_FILE_ULID), SENT_BY))
                .isInstanceOf(NotSendableException.class);
        verify(notificationService, never()).send(any());
    }

    @Test
    void published_butNoEmail_throwsMissingPatientEmail_andSendsNothing() {
        when(labFileApi.requireById(new LabFileId(LAB_FILE_ULID)))
                .thenReturn(summary(LabFileStatus.PUBLISHED));
        when(compteRenduApi.renderCompteRendu(new LabFileId(LAB_FILE_ULID)))
                .thenReturn(new CompteRenduPdf(PDF_BYTES, "cr.pdf"));
        when(pdfPasswordProtector.protect(any(), any())).thenReturn(ENCRYPTED_BYTES);
        when(patientApi.contactFor(new PatientId(PATIENT_ULID)))
                .thenReturn(new RecipientContact(null, "+22890000000", null));

        assertThatThrownBy(() -> service.send(new LabFileId(LAB_FILE_ULID), SENT_BY))
                .isInstanceOf(MissingPatientEmailException.class);
        verify(notificationService, never()).send(any());
    }

    private void stubPublishedDossier() {
        when(labFileApi.requireById(new LabFileId(LAB_FILE_ULID)))
                .thenReturn(summary(LabFileStatus.PUBLISHED));
        when(compteRenduApi.renderCompteRendu(new LabFileId(LAB_FILE_ULID)))
                .thenReturn(new CompteRenduPdf(PDF_BYTES, "cr.pdf"));
        when(pdfPasswordProtector.protect(eq(PDF_BYTES), any())).thenReturn(ENCRYPTED_BYTES);
    }

    private static LabFileSummary summary(LabFileStatus status) {
        return new LabFileSummary(
                new LabFileId(LAB_FILE_ULID),
                new LabFileRegistrationNumber("LF-260529-0001"),
                new PatientSnapshot(PATIENT_ULID, "P001", "Doe", "Jane", Sex.F),
                null,
                null,
                false,
                status,
                List.of(),
                List.of(),
                null,
                null,
                new BillingBlock(BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                Instant.parse("2026-05-29T09:00:00Z"),
                null,
                null,
                null);
    }
}
