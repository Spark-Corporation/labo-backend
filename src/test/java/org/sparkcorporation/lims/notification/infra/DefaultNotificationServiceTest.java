package org.sparkcorporation.lims.notification.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.sparkcorporation.lims.notification.api.Attachment;
import org.sparkcorporation.lims.notification.api.Channel;
import org.sparkcorporation.lims.notification.api.ContextRef;
import org.sparkcorporation.lims.notification.api.ContextType;
import org.sparkcorporation.lims.notification.api.DeliveryStatus;
import org.sparkcorporation.lims.notification.api.EmailRequest;
import org.sparkcorporation.lims.notification.api.WhatsAppRequest;
import org.sparkcorporation.lims.notification.domain.NotificationOutboxEntry;

@ExtendWith(MockitoExtension.class)
class DefaultNotificationServiceTest {

    @Mock private NotificationOutboxRepository repo;
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-29T09:00:00Z"), ZoneOffset.UTC);

    private DefaultNotificationService service;

    @BeforeEach
    void setUp() {
        service = new DefaultNotificationService(repo, clock);
    }

    @Test
    void sendEmailPersistsPendingEntryAndReturnsId() {
        when(repo.save(any(NotificationOutboxEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        var ctx = new ContextRef(ContextType.QUOTE, "01HZX0QUOTE000000000000001");
        var req =
                new EmailRequest(
                        "a@b.test",
                        "Subject",
                        "Body",
                        new Attachment("q.pdf", "application/pdf", new byte[] {1, 2}),
                        ctx);

        var id = service.send(req);

        var captor = ArgumentCaptor.forClass(NotificationOutboxEntry.class);
        org.mockito.Mockito.verify(repo).save(captor.capture());
        var saved = captor.getValue();
        assertThat(id.value()).isEqualTo(saved.getId());
        assertThat(saved.getChannel()).isEqualTo(Channel.EMAIL);
        assertThat(saved.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(saved.getAttempts()).isZero();
        assertThat(saved.getRecipient()).isEqualTo("a@b.test");
        assertThat(saved.getSubject()).isEqualTo("Subject");
        assertThat(saved.getBody()).isEqualTo("Body");
        assertThat(saved.getAttachmentFilename()).isEqualTo("q.pdf");
        assertThat(saved.getAttachmentBytes()).isEqualTo(new byte[] {1, 2});
        assertThat(saved.getContextType()).isEqualTo(ContextType.QUOTE);
        assertThat(saved.getContextId()).isEqualTo("01HZX0QUOTE000000000000001");
        assertThat(saved.getCreatedAt()).isEqualTo(clock.instant());
        assertThat(saved.getNextAttemptAt()).isEqualTo(clock.instant());
    }

    @Test
    void sendWhatsAppPersistsPendingEntry() {
        when(repo.save(any(NotificationOutboxEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        var req =
                new WhatsAppRequest(
                        "+22890000000",
                        "Body",
                        new ContextRef(ContextType.LAB_FILE, "01HZX0LABFILE0000000000001"));

        var id = service.send(req);

        assertThat(id.value()).isNotBlank();
    }

    @Test
    void sendRejectsBlankRecipient() {
        var req =
                new EmailRequest(
                        "  ",
                        "Subject",
                        "Body",
                        null,
                        new ContextRef(ContextType.QUOTE, "01HZX0QUOTE000000000000001"));

        assertThatThrownBy(() -> service.send(req)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void sendRejectsEmailWithoutSubject() {
        var req =
                new EmailRequest(
                        "a@b.test",
                        "   ",
                        "Body",
                        null,
                        new ContextRef(ContextType.QUOTE, "01HZX0QUOTE000000000000001"));

        assertThatThrownBy(() -> service.send(req)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void sendRejectsNullContext() {
        var req = new WhatsAppRequest("+22890000000", "Body", null);

        assertThatThrownBy(() -> service.send(req)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void sendRejectsContextWithNullType() {
        var req =
                new WhatsAppRequest(
                        "+22890000000", "Body", new ContextRef(null, "01HZX0QUOTE000000000000001"));

        assertThatThrownBy(() -> service.send(req)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void sendRejectsContextWithBlankId() {
        var req =
                new WhatsAppRequest(
                        "+22890000000", "Body", new ContextRef(ContextType.QUOTE, "  "));

        assertThatThrownBy(() -> service.send(req)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo);
    }
}
