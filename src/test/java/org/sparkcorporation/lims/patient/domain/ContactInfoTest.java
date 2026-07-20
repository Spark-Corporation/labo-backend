package org.sparkcorporation.lims.patient.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ContactInfoTest {

    @Test
    void allows_both_null() {
        var c = new ContactInfo(null, null);
        assertThat(c.phone()).isNull();
        assertThat(c.email()).isNull();
    }

    @Test
    void valid_construction() {
        var c = new ContactInfo("+22890000000", "x@y.z");
        assertThat(c.phone()).isEqualTo("+22890000000");
        assertThat(c.email()).isEqualTo("x@y.z");
    }

    @Test
    void equals_null_is_false() {
        ContactInfo c = new ContactInfo(null, null);
        assertThat(c.equals(null)).isFalse();
    }
}
