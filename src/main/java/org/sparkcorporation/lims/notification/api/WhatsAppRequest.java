package org.sparkcorporation.lims.notification.api;

/** A WhatsApp message — no subject, no attachment. */
public record WhatsAppRequest(String recipient, String body, ContextRef context)
        implements NotificationRequest {

    @Override
    public Channel channel() {
        return Channel.WHATSAPP;
    }
}
