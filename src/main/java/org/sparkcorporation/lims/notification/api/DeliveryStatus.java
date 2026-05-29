package org.sparkcorporation.lims.notification.api;

/** Lifecycle of an outbox entry: enqueued, delivered, retryable failure, or given up on. */
public enum DeliveryStatus {
    PENDING,
    SENT,
    FAILED,
    ABANDONED
}
