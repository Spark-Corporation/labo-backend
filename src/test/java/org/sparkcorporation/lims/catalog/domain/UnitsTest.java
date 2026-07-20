package org.sparkcorporation.lims.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class UnitsTest {

    @Test
    void accept_zero_decimals() {
        var u = new Units("mg/dL", 0, null, null, null);
        assertThat(u.unit1Decimals()).isZero();
    }

    @Test
    void accept_max_5_decimals() {
        var u = new Units("mg/dL", 5, "g/L", 5, new BigDecimal("0.01"));
        assertThat(u.unit1Decimals()).isEqualTo(5);
        assertThat(u.unit2Decimals()).isEqualTo(5);
    }

    @Test
    void reject_negative_decimals() {
        assertThatThrownBy(() -> new Units("mg/dL", -1, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unit1Decimals");
        assertThatThrownBy(() -> new Units(null, null, "g/L", -1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unit2Decimals");
    }

    @Test
    void reject_more_than_5_decimals() {
        assertThatThrownBy(() -> new Units("mg/dL", 6, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unit1Decimals");
        assertThatThrownBy(() -> new Units(null, null, "g/L", 6, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unit2Decimals");
    }

    @Test
    void empty_returns_all_null() {
        var u = Units.empty();
        assertThat(u.unit1()).isNull();
        assertThat(u.unit1Decimals()).isNull();
        assertThat(u.unit2()).isNull();
        assertThat(u.unit2Decimals()).isNull();
        assertThat(u.conversionFactor()).isNull();
    }
}
