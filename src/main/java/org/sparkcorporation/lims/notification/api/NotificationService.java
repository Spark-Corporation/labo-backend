package org.sparkcorporation.lims.notification.api;

/** Entry point of the {@code notification} module — the only public way to send a message. */
public interface NotificationService {

    /**
     * Persists the request as a {@code PENDING} outbox entry in the caller's transaction; the
     * scheduled poller performs the actual send. Returns the ledger id.
     */
    NotificationId send(NotificationRequest request);
}
