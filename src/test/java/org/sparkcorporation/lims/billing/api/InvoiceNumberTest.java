package org.sparkcorporation.lims.billing.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Compact-constructor regex validation for {@link InvoiceNumber} per spec §5.5. */
class InvoiceNumberTest {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "LAB26-0513-001",
                "LTA24-1710-005",
                "LBA99-1231-9999",
                "LAB00-0101-123456" // ≥ 3 digit seq accepted; arbitrary length OK
            })
    void accepts_valid_invoice_numbers(String number) {
        var n = new InvoiceNumber(number);
        assertThat(n.value()).isEqualTo(number);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "", // empty
                "lab26-0513-001", // lowercase lab code
                "LA26-0513-001", // 2-letter lab code
                "LABC-0513-001", // letter year (regex needs 2 digits)
                "LAB26-051-001", // 3-digit MMdd
                "LAB26-0513-01", // 2-digit seq (need ≥ 3)
                "LAB260513-001", // missing separator
                "LAB26-0513-00A", // alpha seq
                "LAB26-0513-001-" // trailing junk
            })
    void rejects_invalid_invoice_numbers(String number) {
        assertThatThrownBy(() -> new InvoiceNumber(number))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid invoice number");
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new InvoiceNumber(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
