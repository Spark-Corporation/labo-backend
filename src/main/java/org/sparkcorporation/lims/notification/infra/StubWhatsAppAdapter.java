package org.sparkcorporation.lims.notification.infra;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.notification.api.Channel;
import org.sparkcorporation.lims.notification.domain.NotificationOutboxEntry;

/**
 * Logging-only WHATSAPP adapter for dev/test. Enabled when {@code lims.notification.whatsapp.stub}
 * is true (the default). Production toggles it off once a real WhatsApp adapter registers behind
 * the {@link ChannelAdapter} SPI.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "lims.notification.whatsapp",
        name = "stub",
        havingValue = "true",
        matchIfMissing = true)
public class StubWhatsAppAdapter implements ChannelAdapter {

    @Override
    public boolean supports(Channel channel) {
        return channel == Channel.WHATSAPP;
    }

    @Override
    public void dispatch(NotificationOutboxEntry entry) {
        // NEVER log recipient/body/attachment — only the ledger id + context id (ULID).
        log.info(
                "STUB whatsapp dispatch notificationId={} contextId={}",
                entry.getId(),
                entry.getContextId());
    }
}
