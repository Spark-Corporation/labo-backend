package org.sparkcorporation.lims.iam.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RoleTest {

    @Test
    void declares_seven_CDC_values_in_order() {
        assertThat(Role.values())
                .containsExactly(
                        Role.ADMIN,
                        Role.LAB_MANAGER,
                        Role.BIOLOGIST,
                        Role.TECHNICIAN,
                        Role.RECEPTIONIST,
                        Role.CASHIER,
                        Role.ACCOUNTANT);
    }

    @Test
    void every_constant_name_fits_VARCHAR_30() {
        for (Role r : Role.values()) {
            assertThat(r.name().length()).isLessThanOrEqualTo(30);
        }
    }
}
