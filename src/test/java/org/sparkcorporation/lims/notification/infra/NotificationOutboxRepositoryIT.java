package org.sparkcorporation.lims.notification.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.notification.api.Channel;
import org.sparkcorporation.lims.notification.api.ContextType;
import org.sparkcorporation.lims.notification.api.DeliveryStatus;
import org.sparkcorporation.lims.notification.domain.NotificationOutboxEntry;

@SpringBootTest
class NotificationOutboxRepositoryIT extends IntegrationTestBase {

    @Autowired private NotificationOutboxRepository repo;

    @Test
    @Transactional
    void roundTripPendingEntry() {
        var now = Instant.parse("2026-05-29T09:00:00Z");
        var entry =
                NotificationOutboxEntry.pending(
                        "01HZX0NOTIF0000000000000A1",
                        Channel.EMAIL,
                        "a@b.test",
                        "Subject",
                        "Body",
                        "q.pdf",
                        "application/pdf",
                        new byte[] {1, 2, 3},
                        ContextType.QUOTE,
                        "01HZX0QUOTE000000000000001",
                        now);
        repo.save(entry);

        var row = repo.findById("01HZX0NOTIF0000000000000A1").orElseThrow();
        assertThat(row.getChannel()).isEqualTo(Channel.EMAIL);
        assertThat(row.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(row.getRecipient()).isEqualTo("a@b.test");
        assertThat(row.getSubject()).isEqualTo("Subject");
        assertThat(row.getBody()).isEqualTo("Body");
        assertThat(row.getAttachmentFilename()).isEqualTo("q.pdf");
        assertThat(row.getAttachmentContentType()).isEqualTo("application/pdf");
        assertThat(row.getAttachmentBytes()).isEqualTo(new byte[] {1, 2, 3});
        assertThat(row.getAttempts()).isZero();
        assertThat(row.getNextAttemptAt()).isEqualTo(now);
        assertThat(row.getContextType()).isEqualTo(ContextType.QUOTE);
        assertThat(row.getContextId()).isEqualTo("01HZX0QUOTE000000000000001");
        assertThat(row.getCreatedAt()).isEqualTo(now);
        assertThat(row.getSentAt()).isNull();
    }

    @Test
    @Transactional
    void claimDueReturnsPendingEntriesDueNow() {
        var now = Instant.parse("2026-05-29T09:00:00Z");
        repo.save(
                NotificationOutboxEntry.pending(
                        "01HZX0NOTIF0000000000000B1",
                        Channel.WHATSAPP,
                        "+22890000000",
                        null,
                        "Body",
                        null,
                        null,
                        null,
                        ContextType.LAB_FILE,
                        "01HZX0LABFILE0000000000001",
                        now));

        var due = repo.claimDue(now, PageRequest.of(0, 50));
        assertThat(due)
                .extracting(NotificationOutboxEntry::getId)
                .contains("01HZX0NOTIF0000000000000B1");
    }
}
