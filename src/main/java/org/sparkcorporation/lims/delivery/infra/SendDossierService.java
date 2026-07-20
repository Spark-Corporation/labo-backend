package org.sparkcorporation.lims.delivery.infra;

import java.security.SecureRandom;
import java.util.Map;

import dev.sparksam.securedx.core.signing.ActionSigner;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.delivery.domain.MissingPatientEmailException;
import org.sparkcorporation.lims.delivery.domain.NotSendableException;
import org.sparkcorporation.lims.notification.api.Attachment;
import org.sparkcorporation.lims.notification.api.ContextRef;
import org.sparkcorporation.lims.notification.api.ContextType;
import org.sparkcorporation.lims.notification.api.EmailRequest;
import org.sparkcorporation.lims.notification.api.NotificationId;
import org.sparkcorporation.lims.notification.api.NotificationService;
import org.sparkcorporation.lims.notification.api.WhatsAppRequest;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.RecipientContact;
import org.sparkcorporation.lims.result.api.CompteRenduApi;
import org.sparkcorporation.lims.result.api.CompteRenduPdf;

/**
 * Staff-triggered result-dossier send (spec §IX, CDC §IV). Requires the dossier to be {@code
 * PUBLISHED}, renders the compte-rendu PDF, encrypts it with a freshly-generated password, and
 * enqueues an EMAIL notification carrying the encrypted PDF. The password travels <em>out of
 * band</em>: if the patient has a phone, a WhatsApp message carries it; otherwise the password is
 * returned to the calling staff member to relay manually. Records the action in the SP3 signing
 * ledger via {@link ActionSigner}.
 *
 * <p><strong>The generated password and the resolved {@link RecipientContact} are secrets/PII —
 * never log them.</strong> Log the lab-file/notification ULIDs only.
 */
@Service
@RequiredArgsConstructor
public class SendDossierService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String EMAIL_SUBJECT = "Votre compte-rendu d'analyses";
    private static final String EMAIL_BODY =
            "Veuillez trouver ci-joint votre compte-rendu au format PDF protégé par mot de passe.";
    private static final String WHATSAPP_BODY_PREFIX =
            "Le mot de passe de votre compte-rendu est : ";

    /** Crockford-style base32 alphabet (no I, L, O, U) for a human-relayable password. */
    private static final char[] PASSWORD_ALPHABET =
            "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    /** Number of characters in a generated dossier password. */
    private static final int PASSWORD_LENGTH = 10;

    private final LabFileApi labFileApi;
    private final CompteRenduApi compteRenduApi;
    private final PdfPasswordProtector pdfPasswordProtector;
    private final PatientApi patientApi;
    private final NotificationService notificationService;
    private final ActionSigner actionSigner;

    private final SecureRandom random = new SecureRandom();

    /**
     * Encrypts and emails the published dossier; delivers the password out-of-band.
     *
     * @return the enqueued notification ids and — only when the patient has no phone — the password
     *     for the calling staff to relay manually
     * @throws NotSendableException if the dossier is not {@code PUBLISHED}
     * @throws MissingPatientEmailException if the patient has no email on file
     */
    @Transactional
    public SendDossierResult send(LabFileId labFileId, @Nullable String sentBy) {
        LabFileSummary summary = labFileApi.requireById(labFileId);
        if (summary.status() != LabFileStatus.PUBLISHED) {
            throw new NotSendableException(labFileId);
        }

        CompteRenduPdf pdf = compteRenduApi.renderCompteRendu(labFileId);
        String password = generatePassword();
        byte[] encrypted = pdfPasswordProtector.protect(pdf.bytes(), password);

        RecipientContact contact = patientApi.contactFor(new PatientId(summary.patient().id()));
        if (contact.email() == null || contact.email().isBlank()) {
            throw new MissingPatientEmailException(labFileId);
        }

        var context = new ContextRef(ContextType.LAB_FILE, labFileId.value());
        var attachment = new Attachment(pdf.filename(), PDF_CONTENT_TYPE, encrypted);
        NotificationId emailId =
                notificationService.send(
                        new EmailRequest(
                                contact.email(), EMAIL_SUBJECT, EMAIL_BODY, attachment, context));

        actionSigner.sign(
                "dossier.send",
                labFileId.value(),
                Map.of("channel", "EMAIL", "notificationId", emailId.value()));

        String phone = phoneOf(contact);
        if (phone != null) {
            NotificationId whatsAppId =
                    notificationService.send(
                            new WhatsAppRequest(phone, WHATSAPP_BODY_PREFIX + password, context));
            return new SendDossierResult(emailId, whatsAppId, null);
        }
        // No phone: hand the password back to staff to relay manually.
        return new SendDossierResult(emailId, null, password);
    }

    private static @Nullable String phoneOf(RecipientContact contact) {
        if (contact.phone() != null && !contact.phone().isBlank()) {
            return contact.phone();
        }
        if (contact.guardianPhone() != null && !contact.guardianPhone().isBlank()) {
            return contact.guardianPhone();
        }
        return null;
    }

    private String generatePassword() {
        var sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_ALPHABET[random.nextInt(PASSWORD_ALPHABET.length)]);
        }
        return sb.toString();
    }

    /**
     * Outcome of a dossier send: the EMAIL notification id, an optional WhatsApp notification id
     * (present iff the password was relayed by WhatsApp), and an optional {@code password} (present
     * <em>only</em> when the patient has no phone and staff must relay it manually).
     */
    public record SendDossierResult(
            NotificationId emailId,
            @Nullable NotificationId whatsAppId,
            @Nullable String password) {}
}
