package org.sparkcorporation.lims.notification.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NotificationRequestTest {

    @Test
    void emailRequestExposesChannelEmailAndOptionalAttachment() {
        var ctx = new ContextRef(ContextType.QUOTE, "01J000000000000000000QT0TE");
        var req =
                new EmailRequest(
                        "a@b.test",
                        "Subject",
                        "Body",
                        new Attachment("q.pdf", "application/pdf", new byte[] {1, 2}),
                        ctx);
        assertThat(req.channel()).isEqualTo(Channel.EMAIL);
        assertThat(req.attachment()).isNotNull();
        assertThat(req.context()).isEqualTo(ctx);
    }

    @Test
    void whatsAppRequestHasNoAttachmentAndChannelWhatsApp() {
        var req =
                new WhatsAppRequest(
                        "+22890000000",
                        "Body",
                        new ContextRef(ContextType.LAB_FILE, "01J000000000000000000ABF1B"));
        assertThat(req.channel()).isEqualTo(Channel.WHATSAPP);
    }
}
