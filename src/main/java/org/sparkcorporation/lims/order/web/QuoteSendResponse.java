package org.sparkcorporation.lims.order.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import org.sparkcorporation.lims.notification.api.NotificationId;

/**
 * Response body for {@code POST /api/v1/quotes/{id}:send} — the enqueued notification ledger ids.
 */
@Schema(
        description =
                "Result of sending a quote to a patient, carrying the ids of the notifications"
                        + " enqueued for each requested channel.")
public record QuoteSendResponse(
        @Schema(
                        description =
                                "Identifiers of the notification ledger entries created for this"
                                        + " send, one per channel.")
                List<NotificationId> notificationIds) {

    public QuoteSendResponse {
        notificationIds = List.copyOf(notificationIds);
    }
}
