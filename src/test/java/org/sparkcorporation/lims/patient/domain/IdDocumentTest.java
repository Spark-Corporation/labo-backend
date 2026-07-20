package org.sparkcorporation.lims.patient.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IdDocumentTest {

    @Test
    void rejects_null_type() {
        assertThatThrownBy(() -> new IdDocument(null, "ABC123"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_null_number() {
        assertThatThrownBy(() -> new IdDocument(IdDocumentType.CARTE_IDENTITE, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_blank_number() {
        assertThatThrownBy(() -> new IdDocument(IdDocumentType.PASSEPORT, "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void valid_construction() {
        var doc = new IdDocument(IdDocumentType.PASSEPORT, "TGO-2024-0042");
        assertThat(doc.type()).isEqualTo(IdDocumentType.PASSEPORT);
        assertThat(doc.number()).isEqualTo("TGO-2024-0042");
    }

    @Test
    void equals_null_is_false() {
        var doc = new IdDocument(IdDocumentType.AUTRE, "x");
        assertThat(doc.equals(null)).isFalse();
    }
}
