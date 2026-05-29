package org.sparkcorporation.lims.notification.api;

import java.time.Instant;

/** Application event emitted by the poller after a successful send. */
public record NotificationDelivered(
        NotificationId notificationId, Channel channel, ContextRef context, Instant occurredAt) {}
