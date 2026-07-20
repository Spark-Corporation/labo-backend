package org.sparkcorporation.lims.patient.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GuardianRelationshipTest {

    @Test
    void declares_four_CDC_values_in_order() {
        assertThat(GuardianRelationship.values())
                .containsExactly(
                        GuardianRelationship.PARENT,
                        GuardianRelationship.TUTEUR,
                        GuardianRelationship.ACCOMPAGNANT,
                        GuardianRelationship.AUTRE);
    }

    @Test
    void every_constant_name_fits_VARCHAR_20() {
        for (GuardianRelationship r : GuardianRelationship.values()) {
            assertThat(r.name().length()).isLessThanOrEqualTo(20);
        }
    }
}
