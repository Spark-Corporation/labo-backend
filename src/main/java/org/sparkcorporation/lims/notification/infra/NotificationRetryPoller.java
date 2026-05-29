package org.sparkcorporation.lims.notification.infra;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.notification.api.ContextRef;
import org.sparkcorporation.lims.notification.api.NotificationDelivered;
import org.sparkcorporation.lims.notification.api.NotificationId;
import org.sparkcorporation.lims.notification.domain.NotificationOutboxEntry;

/**
 * Scheduled worker that drains the {@code notification_outbox}. Each tick claims a batch of due
 * entries (PENDING / FAILED with {@code nextAttemptAt <= now}), dispatches each through the
 * matching {@link ChannelAdapter}, and on success marks it SENT and emits {@link
 * NotificationDelivered}. A dispatch failure re-queues the entry with exponential backoff (1→16 min
 * off a 1-minute base) until {@code maxAttempts}, after which it is ABANDONED. Scheduling is
 * enabled app-wide via {@code pdf/infra/PdfRenderExecutorConfig}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRetryPoller {

    private static final int MAX_ERROR_LENGTH = 1000;

    private final NotificationOutboxRepository repo;
    private final List<ChannelAdapter> adapters;
    private final ApplicationEventPublisher events;
    private final NotificationProperties props;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${lims.notification.poll-interval:30s}")
    @Transactional
    public void poll() {
        var now = clock.instant();
        var due = repo.claimDue(now, PageRequest.of(0, props.getBatchSize()));
        for (var entry : due) {
            dispatchOne(entry, now);
        }
    }

    private void dispatchOne(NotificationOutboxEntry entry, Instant now) {
        var adapter = adapters.stream().filter(a -> a.supports(entry.getChannel())).findFirst();
        if (adapter.isEmpty()) {
            // Leave PENDING; a real adapter for this channel may register later.
            log.warn(
                    "No adapter for channel={} notificationId={}",
                    entry.getChannel(),
                    entry.getId());
            return;
        }
        // Context is validated non-null at enqueue time (DefaultNotificationService), so this is a
        // true invariant; resolving it outside the dispatch try keeps an unexpected null from being
        // misclassified as a delivery failure and looping the entry through retries.
        ContextRef context = Objects.requireNonNull(entry.toContextRef());
        try {
            adapter.get().dispatch(entry);
            entry.markSent(now);
            events.publishEvent(
                    new NotificationDelivered(
                            new NotificationId(entry.getId()), entry.getChannel(), context, now));
        } catch (RuntimeException ex) {
            int attempts = entry.getAttempts() + 1;
            String error = truncate(ex.getMessage());
            if (attempts >= props.getMaxAttempts()) {
                entry.markAbandoned(attempts, error);
                log.error(
                        "Notification abandoned after {} attempts notificationId={} contextId={}",
                        attempts,
                        entry.getId(),
                        entry.getContextId());
            } else {
                entry.markFailed(attempts, now.plus(backoff(attempts)), error);
                log.warn(
                        "Notification dispatch failed attempt={} notificationId={} contextId={}",
                        attempts,
                        entry.getId(),
                        entry.getContextId());
            }
        }
    }

    /** Exponential backoff off the configured base: 1→2→4→8→16 × base for attempts 1..5. */
    private Duration backoff(int attempts) {
        long mult = 1L << (attempts - 1);
        return props.getBackoffBase().multipliedBy(mult);
    }

    private static @Nullable String truncate(@Nullable String msg) {
        if (msg == null) {
            return null;
        }
        return msg.length() > MAX_ERROR_LENGTH ? msg.substring(0, MAX_ERROR_LENGTH) : msg;
    }
}
