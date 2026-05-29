package org.sparkcorporation.lims.order.web;

import java.util.List;

import org.sparkcorporation.lims.notification.api.NotificationId;

/**
 * Response body for {@code POST /api/v1/quotes/{id}:send} — the enqueued notification ledger ids.
 */
public record QuoteSendResponse(List<NotificationId> notificationIds) {

    public QuoteSendResponse {
        notificationIds = List.copyOf(notificationIds);
    }
}
