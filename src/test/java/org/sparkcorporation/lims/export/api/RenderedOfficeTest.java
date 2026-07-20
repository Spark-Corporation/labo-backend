package org.sparkcorporation.lims.export.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class RenderedOfficeTest {

    @Test
    void bytes_are_defensively_copied_on_construction_and_on_read() {
        byte[] source = {1, 2, 3};
        var rendered = new RenderedOffice(source, "f.docx", OfficeFormat.DOCX, Instant.EPOCH);

        source[0] = 99; // mutate caller's array
        assertThat(rendered.bytes()[0]).isEqualTo((byte) 1); // construction copy held

        rendered.bytes()[1] = 88; // mutate a read copy
        assertThat(rendered.bytes()[1]).isEqualTo((byte) 2); // accessor copy held
    }
}
