package org.sparkcorporation.lims.notification.api;

import org.jspecify.annotations.Nullable;

/** An email message with a subject and at most one attachment. */
public record EmailRequest(
        String recipient,
        String subject,
        String body,
        @Nullable Attachment attachment,
        ContextRef context)
        implements NotificationRequest {

    @Override
    public Channel channel() {
        return Channel.EMAIL;
    }
}
