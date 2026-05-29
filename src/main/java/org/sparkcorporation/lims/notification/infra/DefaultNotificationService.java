package org.sparkcorporation.lims.notification.infra;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.notification.api.Channel;
import org.sparkcorporation.lims.notification.api.ContextRef;
import org.sparkcorporation.lims.notification.api.EmailRequest;
import org.sparkcorporation.lims.notification.api.NotificationId;
import org.sparkcorporation.lims.notification.api.NotificationRequest;
import org.sparkcorporation.lims.notification.api.NotificationService;
import org.sparkcorporation.lims.notification.domain.NotificationOutboxEntry;

/**
 * Persists a {@link NotificationRequest} as a {@code PENDING} outbox entry in the caller's
 * transaction (spec §4.1). No send happens inline — the {@code NotificationRetryPoller} (Chunk 2)
 * performs the actual delivery, so the trigger endpoint commits fast and a provider outage never
 * fails the staff action.
 */
@Service
@RequiredArgsConstructor
class DefaultNotificationService implements NotificationService {

    private final NotificationOutboxRepository repository;
    private final Clock clock;

    @Override
    @Transactional
    public NotificationId send(NotificationRequest request) {
        if (request.recipient().isBlank()) {
            throw new IllegalArgumentException("notification recipient must not be blank");
        }

        ContextRef context = request.context();
        if (context == null) {
            throw new IllegalArgumentException("notification context must not be null");
        }
        if (context.type() == null) {
            throw new IllegalArgumentException("notification context type must not be null");
        }
        if (context.id() == null || context.id().isBlank()) {
            throw new IllegalArgumentException("notification context id must not be blank");
        }

        String subject = null;
        String attachmentFilename = null;
        String attachmentContentType = null;
        byte[] attachmentBytes = null;
        if (request instanceof EmailRequest email) {
            if (email.subject().isBlank()) {
                throw new IllegalArgumentException("email notification must carry a subject");
            }
            subject = email.subject();
            var attachment = email.attachment();
            if (attachment != null) {
                attachmentFilename = attachment.filename();
                attachmentContentType = attachment.contentType();
                attachmentBytes = attachment.bytes();
            }
        }

        var now = clock.instant();
        var id = NotificationId.next();
        Channel channel = request.channel();
        var entry =
                NotificationOutboxEntry.pending(
                        id.value(),
                        channel,
                        request.recipient(),
                        subject,
                        request.body(),
                        attachmentFilename,
                        attachmentContentType,
                        attachmentBytes,
                        context.type(),
                        context.id(),
                        now);
        repository.save(entry);
        return id;
    }
}
