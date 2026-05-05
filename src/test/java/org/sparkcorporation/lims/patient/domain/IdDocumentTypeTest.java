package org.sparkcorporation.lims.patient.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IdDocumentTypeTest {

    @Test
    void declares_four_values_in_order() {
        assertThat(IdDocumentType.values())
                .containsExactly(
                        IdDocumentType.CARTE_IDENTITE,
                        IdDocumentType.PASSEPORT,
                        IdDocumentType.PERMIS_CONDUIRE,
                        IdDocumentType.AUTRE);
    }

    @Test
    void every_constant_name_fits_VARCHAR_20() {
        for (IdDocumentType t : IdDocumentType.values()) {
            assertThat(t.name().length()).isLessThanOrEqualTo(20);
        }
    }
}
