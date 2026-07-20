package org.sparkcorporation.lims.attachment.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AttachmentIdTest {

    @Test
    void acceptsAValidUlid() {
        var id = new AttachmentId("01HZX0000000000000000AT001");
        assertThat(id.value()).isEqualTo("01HZX0000000000000000AT001");
    }

    @Test
    void rejectsANonUlid() {
        assertThatThrownBy(() -> new AttachmentId("not-a-ulid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new AttachmentId(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void nextGeneratesAValidUlid() {
        assertThat(AttachmentId.next().value()).hasSize(26);
    }
}
