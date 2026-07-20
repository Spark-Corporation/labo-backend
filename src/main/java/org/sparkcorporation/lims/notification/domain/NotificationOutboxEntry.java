package org.sparkcorporation.lims.notification.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.notification.api.Channel;
import org.sparkcorporation.lims.notification.api.ContextRef;
import org.sparkcorporation.lims.notification.api.ContextType;
import org.sparkcorporation.lims.notification.api.DeliveryStatus;

/**
 * Durable outbox ledger row — the authoritative record of a notification's delivery state (status,
 * attempts, last error, timestamps). The {@code recipient}, {@code body}, and {@code
 * attachmentBytes} are PII and must never be written to logs.
 */
@Entity
@Table(name = "notification_outbox")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class NotificationOutboxEntry {

    @Id
    @Column(name = "id", length = 26, nullable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 16, nullable = false)
    private Channel channel;

    @Column(name = "recipient", length = 255, nullable = false)
    private String recipient;

    @Column(name = "subject", length = 255)
    private @Nullable String subject;

    @Column(name = "body", columnDefinition = "text", nullable = false)
    private String body;

    @Column(name = "attachment_filename", length = 255)
    private @Nullable String attachmentFilename;

    @Column(name = "attachment_content_type", length = 100)
    private @Nullable String attachmentContentType;

    @Column(name = "attachment_bytes")
    private byte @Nullable [] attachmentBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private DeliveryStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", columnDefinition = "text")
    private @Nullable String lastError;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", length = 16)
    private @Nullable ContextType contextType;

    @Column(name = "context_id", length = 26)
    private @Nullable String contextId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private @Nullable Instant sentAt;

    /** Builds a fresh {@code PENDING} entry due immediately (its {@code nextAttemptAt == now}). */
    public static NotificationOutboxEntry pending(
            String id,
            Channel channel,
            String recipient,
            @Nullable String subject,
            String body,
            @Nullable String attachmentFilename,
            @Nullable String attachmentContentType,
            byte @Nullable [] attachmentBytes,
            @Nullable ContextType contextType,
            @Nullable String contextId,
            Instant now) {
        var entry = new NotificationOutboxEntry();
        entry.id = id;
        entry.channel = channel;
        entry.recipient = recipient;
        entry.subject = subject;
        entry.body = body;
        entry.attachmentFilename = attachmentFilename;
        entry.attachmentContentType = attachmentContentType;
        entry.attachmentBytes = attachmentBytes;
        entry.status = DeliveryStatus.PENDING;
        entry.attempts = 0;
        entry.nextAttemptAt = now;
        entry.contextType = contextType;
        entry.contextId = contextId;
        entry.createdAt = now;
        return entry;
    }

    /** Marks the entry delivered: status {@code SENT}, {@code sentAt = now}, error cleared. */
    public void markSent(Instant now) {
        this.status = DeliveryStatus.SENT;
        this.sentAt = now;
        this.lastError = null;
    }

    /**
     * Records a retryable failure: status {@code FAILED}, bumps {@code attempts} to {@code
     * newAttempts}, re-queues at {@code nextAttemptAt}, and stores the (already truncated) error.
     */
    public void markFailed(int newAttempts, Instant nextAttemptAt, @Nullable String error) {
        this.status = DeliveryStatus.FAILED;
        this.attempts = newAttempts;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = error;
    }

    /**
     * Gives up after exhausting the retry budget: status {@code ABANDONED}, attempts bumped, the
     * (already truncated) error stored. The entry is never re-claimed (claim query only sees {@code
     * PENDING}/{@code FAILED}).
     */
    public void markAbandoned(int newAttempts, @Nullable String error) {
        this.status = DeliveryStatus.ABANDONED;
        this.attempts = newAttempts;
        this.lastError = error;
    }

    /** Reconstructs the {@link ContextRef} for the {@code NotificationDelivered} event. */
    public @Nullable ContextRef toContextRef() {
        if (contextType == null || contextId == null) {
            return null;
        }
        return new ContextRef(contextType, contextId);
    }
}
