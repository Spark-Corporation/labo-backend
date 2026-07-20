package org.sparkcorporation.lims.notification.infra;

import org.sparkcorporation.lims.notification.api.Channel;
import org.sparkcorporation.lims.notification.domain.NotificationOutboxEntry;

/**
 * Service-provider interface for the actual outbound send of a notification. One implementation per
 * channel; the poller picks the first adapter whose {@link #supports(Channel)} matches the entry's
 * channel. Stub adapters (logging-only) ship for dev/test; real SMTP / WhatsApp adapters register
 * later behind the same SPI without touching the poller.
 */
public interface ChannelAdapter {

    /** True when this adapter handles the given channel. */
    boolean supports(Channel channel);

    /**
     * Performs the actual send. Throwing a {@link RuntimeException} signals a retryable failure to
     * the poller (it applies backoff and re-queues until {@code maxAttempts}).
     */
    void dispatch(NotificationOutboxEntry entry);
}
