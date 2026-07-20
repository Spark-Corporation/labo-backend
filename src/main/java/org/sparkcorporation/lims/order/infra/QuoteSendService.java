package org.sparkcorporation.lims.order.infra;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.notification.api.Attachment;
import org.sparkcorporation.lims.notification.api.Channel;
import org.sparkcorporation.lims.notification.api.ContextRef;
import org.sparkcorporation.lims.notification.api.ContextType;
import org.sparkcorporation.lims.notification.api.EmailRequest;
import org.sparkcorporation.lims.notification.api.NotificationId;
import org.sparkcorporation.lims.notification.api.NotificationRequest;
import org.sparkcorporation.lims.notification.api.NotificationService;
import org.sparkcorporation.lims.notification.api.WhatsAppRequest;
import org.sparkcorporation.lims.order.api.QuoteApi;
import org.sparkcorporation.lims.order.api.QuoteId;
import org.sparkcorporation.lims.order.api.QuoteStatus;
import org.sparkcorporation.lims.order.api.QuoteSummary;
import org.sparkcorporation.lims.order.domain.MissingRecipientContactException;
import org.sparkcorporation.lims.order.domain.QuoteNotSendableException;
import org.sparkcorporation.lims.order.infra.pdf.QuoteReportModelAssembler;
import org.sparkcorporation.lims.order.infra.pdf.QuoteReportPdfDocument;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.RecipientContact;
import org.sparkcorporation.lims.pdf.api.PdfRenderingService;

/**
 * Staff-triggered quote send (spec §4.2). Renders the quote PDF, resolves the patient's contact,
 * and enqueues one {@code notification_outbox} entry per requested channel — the actual delivery is
 * performed asynchronously by the {@code NotificationRetryPoller}.
 *
 * <p>Only {@code LOCKED} or {@code CONVERTED} quotes are sendable: a {@code DRAFT}/{@code EXPIRED}
 * quote carries no contractual pricing and raises {@link QuoteNotSendableException}.
 *
 * <p>The resolved {@link RecipientContact} is PII — never log its fields; log the quote/patient
 * ULID instead.
 */
@Service
@RequiredArgsConstructor
public class QuoteSendService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String EMAIL_SUBJECT = "Votre devis";
    private static final String EMAIL_BODY = "Veuillez trouver ci-joint votre devis au format PDF.";
    private static final String WHATSAPP_BODY =
            "Votre devis est disponible. Contactez le laboratoire pour le recevoir.";

    private final QuoteApi quoteApi;
    private final QuoteReportModelAssembler assembler;
    private final QuoteReportPdfDocument docType;
    private final PdfRenderingService pdfService;
    private final PatientApi patientApi;
    private final NotificationService notificationService;

    /**
     * Renders the quote PDF and enqueues one notification per distinct requested channel.
     *
     * @return the ledger ids of the enqueued notifications, one per requested channel
     * @throws QuoteNotSendableException if the quote is not {@code LOCKED}/{@code CONVERTED}
     * @throws MissingRecipientContactException if a requested channel has no usable address
     */
    @Transactional
    public List<NotificationId> send(QuoteId quoteId, Set<Channel> channels) {
        QuoteSummary quote = quoteApi.requireById(quoteId);
        if (quote.status() != QuoteStatus.LOCKED && quote.status() != QuoteStatus.CONVERTED) {
            throw new QuoteNotSendableException(
                    "quote "
                            + quoteId.value()
                            + " is "
                            + quote.status()
                            + "; only LOCKED or CONVERTED quotes can be sent");
        }

        // Resolve the recipient contact and verify each requested channel has a usable address
        // BEFORE rendering the PDF (spec §5.1 ordering): a missing-contact 422 must short-circuit
        // before the expensive assemble/render step rather than waste a render.
        var contact = patientApi.contactFor(new PatientId(quote.patient().id()));
        var addresses = new EnumMap<Channel, String>(Channel.class);
        for (Channel channel : channels) {
            addresses.put(channel, resolveAddress(channel, contact));
        }

        // The PDF attachment is only needed by the EMAIL channel.
        Attachment attachment = null;
        if (channels.contains(Channel.EMAIL)) {
            var model = assembler.assemble(quoteId);
            var rendered = pdfService.render(docType, model);
            attachment =
                    new Attachment(
                            rendered.suggestedFilename(), PDF_CONTENT_TYPE, rendered.bytes());
        }

        var context = new ContextRef(ContextType.QUOTE, quoteId.value());
        var ids = new ArrayList<NotificationId>();
        for (Channel channel : channels) {
            ids.add(
                    notificationService.send(
                            buildRequest(channel, addresses.get(channel), attachment, context)));
        }
        return List.copyOf(ids);
    }

    private static NotificationRequest buildRequest(
            Channel channel, String address, @Nullable Attachment attachment, ContextRef context) {
        return switch (channel) {
            case EMAIL -> new EmailRequest(address, EMAIL_SUBJECT, EMAIL_BODY, attachment, context);
            case WHATSAPP -> new WhatsAppRequest(address, WHATSAPP_BODY, context);
        };
    }

    private static String resolveAddress(Channel channel, RecipientContact contact) {
        return switch (channel) {
            case EMAIL -> require(contact.email(), channel);
            case WHATSAPP -> {
                String phone = contact.phone() != null ? contact.phone() : contact.guardianPhone();
                yield require(phone, channel);
            }
        };
    }

    private static String require(@Nullable String address, Channel channel) {
        if (address == null || address.isBlank()) {
            throw new MissingRecipientContactException(
                    "patient has no usable contact for channel " + channel);
        }
        return address;
    }
}
