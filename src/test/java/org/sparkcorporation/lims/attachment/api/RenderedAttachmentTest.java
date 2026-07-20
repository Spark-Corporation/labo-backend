package org.sparkcorporation.lims.attachment.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RenderedAttachmentTest {

    @Test
    void clonesBytesOnConstructionAndAccess() {
        byte[] original = {'%', 'P', 'D', 'F'};
        var rendered = new RenderedAttachment(original, "application/pdf", "scan.pdf");
        original[0] = 'X';
        assertThat(rendered.bytes()).containsExactly('%', 'P', 'D', 'F');
        rendered.bytes()[0] = 'X';
        assertThat(rendered.bytes()).containsExactly('%', 'P', 'D', 'F');
    }
}
