package org.sparkcorporation.lims.notification.api;

/**
 * A fully-formed outbound message handed to {@link NotificationService}. The {@code notification}
 * module knows nothing about quotes or results; callers build the concrete request themselves.
 */
public sealed interface NotificationRequest permits EmailRequest, WhatsAppRequest {

    Channel channel();

    String recipient();

    String body();

    ContextRef context();
}
