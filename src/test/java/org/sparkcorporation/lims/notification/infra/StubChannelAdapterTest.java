package org.sparkcorporation.lims.notification.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.notification.api.Channel;
import org.sparkcorporation.lims.notification.api.ContextType;
import org.sparkcorporation.lims.notification.domain.NotificationOutboxEntry;

class StubChannelAdapterTest {

    private static NotificationOutboxEntry entry(Channel channel) {
        return NotificationOutboxEntry.pending(
                "01HZX0NOTIF0000000000000A1",
                channel,
                channel == Channel.EMAIL ? "a@b.test" : "+22890000000",
                channel == Channel.EMAIL ? "Subject" : null,
                "Body",
                null,
                null,
                null,
                ContextType.QUOTE,
                "01HZX0QUOTE000000000000001",
                Instant.parse("2026-05-29T09:00:00Z"));
    }

    @Test
    void emailAdapterSupportsOnlyEmail() {
        var adapter = new StubEmailAdapter();
        assertThat(adapter.supports(Channel.EMAIL)).isTrue();
        assertThat(adapter.supports(Channel.WHATSAPP)).isFalse();
    }

    @Test
    void emailAdapterDispatchesWithoutThrowing() {
        var adapter = new StubEmailAdapter();
        assertThatCode(() -> adapter.dispatch(entry(Channel.EMAIL))).doesNotThrowAnyException();
    }

    @Test
    void whatsAppAdapterSupportsOnlyWhatsApp() {
        var adapter = new StubWhatsAppAdapter();
        assertThat(adapter.supports(Channel.WHATSAPP)).isTrue();
        assertThat(adapter.supports(Channel.EMAIL)).isFalse();
    }

    @Test
    void whatsAppAdapterDispatchesWithoutThrowing() {
        var adapter = new StubWhatsAppAdapter();
        assertThatCode(() -> adapter.dispatch(entry(Channel.WHATSAPP))).doesNotThrowAnyException();
    }
}
