package org.sparkcorporation.lims.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaymentModeTest {

    @Test
    void declares_five_CDC_modes_in_order() {
        assertThat(PaymentMode.values())
                .containsExactly(
                        PaymentMode.CHEQUE,
                        PaymentMode.ESPECE,
                        PaymentMode.TPE,
                        PaymentMode.FLOOZ,
                        PaymentMode.MIXX_BY_YAS);
    }

    @Test
    void every_constant_name_fits_VARCHAR_20_storage() {
        for (PaymentMode m : PaymentMode.values()) {
            assertThat(m.name().length())
                    .as("PaymentMode '%s' must fit VARCHAR(20)", m.name())
                    .isLessThanOrEqualTo(20);
        }
    }
}
