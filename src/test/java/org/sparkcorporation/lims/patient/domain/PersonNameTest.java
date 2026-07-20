package org.sparkcorporation.lims.patient.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PersonNameTest {

    @Test
    void rejects_null_lastName() {
        assertThatThrownBy(() -> new PersonName(null, "Jane", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_blank_lastName() {
        assertThatThrownBy(() -> new PersonName("   ", "Jane", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void allows_null_firstNames_and_maidenName() {
        var name = new PersonName("DOE", null, null);
        assertThat(name.lastName()).isEqualTo("DOE");
        assertThat(name.firstNames()).isNull();
        assertThat(name.maidenName()).isNull();
    }

    @Test
    void record_equality_holds() {
        assertThat(new PersonName("DOE", "Jane", null))
                .isEqualTo(new PersonName("DOE", "Jane", null));
    }

    @Test
    void equals_null_is_false() {
        PersonName n = new PersonName("DOE", null, null);
        assertThat(n.equals(null)).isFalse();
    }
}
