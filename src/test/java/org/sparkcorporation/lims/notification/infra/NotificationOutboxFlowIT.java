package org.sparkcorporation.lims.notification.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.notification.api.Attachment;
import org.sparkcorporation.lims.notification.api.Channel;
import org.sparkcorporation.lims.notification.api.ContextRef;
import org.sparkcorporation.lims.notification.api.ContextType;
import org.sparkcorporation.lims.notification.api.DeliveryStatus;
import org.sparkcorporation.lims.notification.api.EmailRequest;
import org.sparkcorporation.lims.notification.api.NotificationDelivered;
import org.sparkcorporation.lims.notification.api.NotificationService;

/**
 * End-to-end module IT: enqueue a notification via {@link NotificationService#send}, drive the
 * {@link NotificationRetryPoller} directly (no waiting on the scheduler), and assert the outbox row
 * flips to {@code SENT} and a {@link NotificationDelivered} event is published. The stub adapters
 * are active by default in the {@code test} profile.
 */
@SpringBootTest
@RecordApplicationEvents
class NotificationOutboxFlowIT extends IntegrationTestBase {

    @Autowired private NotificationService notificationService;
    @Autowired private NotificationRetryPoller poller;
    @Autowired private NotificationOutboxRepository repo;

    @Test
    void enqueuedEmailIsDeliveredAndStampedSent(ApplicationEvents events) {
        var ctx = new ContextRef(ContextType.QUOTE, "01KST3G974409D3SXGQH04209A");
        var req =
                new EmailRequest(
                        "a@b.test",
                        "Subject",
                        "Body",
                        new Attachment("q.pdf", "application/pdf", new byte[] {1, 2}),
                        ctx);

        var id = notificationService.send(req);

        // Pre-condition: persisted PENDING.
        var before = repo.findById(id.value()).orElseThrow();
        assertThat(before.getStatus()).isEqualTo(DeliveryStatus.PENDING);

        poller.poll();

        var after = repo.findById(id.value()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(DeliveryStatus.SENT);
        assertThat(after.getSentAt()).isNotNull();

        var delivered = events.stream(NotificationDelivered.class).toList();
        assertThat(delivered)
                .anySatisfy(
                        evt -> {
                            assertThat(evt.notificationId().value()).isEqualTo(id.value());
                            assertThat(evt.channel()).isEqualTo(Channel.EMAIL);
                            assertThat(evt.context()).isEqualTo(ctx);
                        });
    }
}
