package org.sparkcorporation.lims.notification.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;

import org.sparkcorporation.lims.notification.api.Channel;
import org.sparkcorporation.lims.notification.api.ContextType;
import org.sparkcorporation.lims.notification.api.DeliveryStatus;
import org.sparkcorporation.lims.notification.api.NotificationDelivered;
import org.sparkcorporation.lims.notification.domain.NotificationOutboxEntry;

@ExtendWith(MockitoExtension.class)
class NotificationRetryPollerTest {

    private static final Instant NOW = Instant.parse("2026-05-29T09:00:00Z");

    @Mock private NotificationOutboxRepository repo;
    @Mock private ChannelAdapter emailAdapter;
    @Mock private ApplicationEventPublisher events;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private NotificationProperties props;
    private NotificationRetryPoller poller;

    @BeforeEach
    void setUp() {
        props = new NotificationProperties();
        poller = new NotificationRetryPoller(repo, List.of(emailAdapter), events, props, clock);
    }

    private static final String NOTIF_ID = "01KST3G2HKZ58WF5MNJ0ZH75AB";
    private static final String QUOTE_ID = "01KST3G974409D3SXGQH04209A";

    private static NotificationOutboxEntry pending() {
        return NotificationOutboxEntry.pending(
                NOTIF_ID,
                Channel.EMAIL,
                "a@b.test",
                "Subject",
                "Body",
                null,
                null,
                null,
                ContextType.QUOTE,
                QUOTE_ID,
                NOW);
    }

    @Test
    void successPathMarksSentAndPublishesEvent() {
        var entry = pending();
        when(emailAdapter.supports(Channel.EMAIL)).thenReturn(true);
        when(repo.claimDue(eq(NOW), any(Pageable.class))).thenReturn(List.of(entry));

        poller.poll();

        assertThat(entry.getStatus()).isEqualTo(DeliveryStatus.SENT);
        assertThat(entry.getSentAt()).isEqualTo(NOW);
        var captor = ArgumentCaptor.forClass(NotificationDelivered.class);
        verify(events).publishEvent(captor.capture());
        var evt = captor.getValue();
        assertThat(evt.notificationId().value()).isEqualTo(NOTIF_ID);
        assertThat(evt.channel()).isEqualTo(Channel.EMAIL);
        assertThat(evt.context().type()).isEqualTo(ContextType.QUOTE);
        assertThat(evt.context().id()).isEqualTo(QUOTE_ID);
        assertThat(evt.occurredAt()).isEqualTo(NOW);
    }

    @Test
    void failurePathMarksFailedIncrementsAttemptsAndSchedulesBackoff() {
        var entry = pending();
        when(emailAdapter.supports(Channel.EMAIL)).thenReturn(true);
        org.mockito.Mockito.doThrow(new RuntimeException("smtp down"))
                .when(emailAdapter)
                .dispatch(entry);
        when(repo.claimDue(eq(NOW), any(Pageable.class))).thenReturn(List.of(entry));

        poller.poll();

        assertThat(entry.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(entry.getAttempts()).isEqualTo(1);
        // backoff(1) = 1 min after a single failure
        assertThat(entry.getNextAttemptAt()).isEqualTo(NOW.plus(Duration.ofMinutes(1)));
        assertThat(entry.getLastError()).isEqualTo("smtp down");
        verify(events, never()).publishEvent(any());
    }

    @Test
    void exhaustionMarksAbandonedAndPublishesNoEvent() {
        var entry = pending();
        // attempts already at maxAttempts-1 = 4
        for (int i = 0; i < 4; i++) {
            entry.markFailed(i + 1, NOW, "boom");
        }
        when(emailAdapter.supports(Channel.EMAIL)).thenReturn(true);
        org.mockito.Mockito.doThrow(new RuntimeException("still down"))
                .when(emailAdapter)
                .dispatch(entry);
        when(repo.claimDue(eq(NOW), any(Pageable.class))).thenReturn(List.of(entry));

        poller.poll();

        assertThat(entry.getStatus()).isEqualTo(DeliveryStatus.ABANDONED);
        assertThat(entry.getLastError()).isEqualTo("still down");
        verify(events, never()).publishEvent(any());
    }

    @Test
    void backoffScheduleDoublesEachAttempt() {
        var entry = pending();
        when(emailAdapter.supports(Channel.EMAIL)).thenReturn(true);
        org.mockito.Mockito.doThrow(new RuntimeException("down"))
                .when(emailAdapter)
                .dispatch(entry);
        when(repo.claimDue(eq(NOW), any(Pageable.class))).thenReturn(List.of(entry));

        long[] expectedMinutes = {1, 2, 4, 8};
        for (int i = 0; i < expectedMinutes.length; i++) {
            poller.poll();
            assertThat(entry.getAttempts()).isEqualTo(i + 1);
            assertThat(entry.getNextAttemptAt())
                    .isEqualTo(NOW.plus(Duration.ofMinutes(expectedMinutes[i])));
        }
        // 5th failure hits maxAttempts -> ABANDONED
        poller.poll();
        assertThat(entry.getStatus()).isEqualTo(DeliveryStatus.ABANDONED);
    }

    @Test
    void noAdapterLeavesEntryPending() {
        var entry = pending();
        when(emailAdapter.supports(Channel.EMAIL)).thenReturn(false);
        when(repo.claimDue(eq(NOW), any(Pageable.class))).thenReturn(List.of(entry));

        poller.poll();

        assertThat(entry.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        verify(events, never()).publishEvent(any());
    }
}
