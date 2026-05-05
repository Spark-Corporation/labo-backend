package org.sparkcorporation.lims.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LabCountryTest {

    @Test
    void TOGO_invoice_prefix_is_LTA() {
        assertThat(LabCountry.TOGO.invoicePrefix()).isEqualTo("LTA");
    }

    @Test
    void BENIN_invoice_prefix_is_LBA() {
        assertThat(LabCountry.BENIN.invoicePrefix()).isEqualTo("LBA");
    }

    @Test
    void only_two_values_declared() {
        assertThat(LabCountry.values()).containsExactly(LabCountry.TOGO, LabCountry.BENIN);
    }
}
