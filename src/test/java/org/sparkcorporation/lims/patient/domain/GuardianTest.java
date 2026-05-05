package org.sparkcorporation.lims.patient.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GuardianTest {

    @Test
    void rejects_null_name() {
        assertThatThrownBy(() -> new Guardian(null, "+228...", GuardianRelationship.PARENT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_blank_name() {
        assertThatThrownBy(() -> new Guardian("  ", null, GuardianRelationship.TUTEUR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_null_relationship() {
        assertThatThrownBy(() -> new Guardian("DOE Sr", null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void allows_null_phone() {
        var g = new Guardian("DOE Sr", null, GuardianRelationship.PARENT);
        assertThat(g.phone()).isNull();
    }

    @Test
    void valid_construction() {
        var g = new Guardian("DOE Sr", "+22890000000", GuardianRelationship.PARENT);
        assertThat(g.name()).isEqualTo("DOE Sr");
        assertThat(g.relationship()).isEqualTo(GuardianRelationship.PARENT);
    }

    @Test
    void equals_null_is_false() {
        var g = new Guardian("X", null, GuardianRelationship.AUTRE);
        assertThat(g.equals(null)).isFalse();
    }
}
