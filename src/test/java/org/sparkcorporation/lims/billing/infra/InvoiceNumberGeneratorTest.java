package org.sparkcorporation.lims.billing.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link InvoiceNumberGenerator} — exercises the format + UPSERT dispatch with a
 * mocked {@link EntityManager}. Concurrent-safety lives in {@code
 * InvoiceNumberGeneratorConcurrencyIT} (Postgres testcontainer).
 */
class InvoiceNumberGeneratorTest {

    private final EntityManager em = mock(EntityManager.class);
    private final LabProperties props = new LabProperties("LTA");
    private final InvoiceNumberGenerator generator = new InvoiceNumberGenerator(em, props);

    @Test
    void generates_invoice_number_with_upsert_returning_value() {
        var query = mock(Query.class);
        when(em.createNativeQuery(org.mockito.ArgumentMatchers.anyString())).thenReturn(query);
        when(query.setParameter(
                        org.mockito.ArgumentMatchers.eq(1),
                        org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(query);
        when(query.setParameter(
                        org.mockito.ArgumentMatchers.eq(2), org.mockito.ArgumentMatchers.any()))
                .thenReturn(query);
        when(query.getSingleResult()).thenReturn(5);

        var number = generator.next(LocalDate.of(2026, 5, 13));

        assertThat(number.value()).isEqualTo("LTA26-1305-005");
    }

    @Test
    void format_matches_cdc_example_for_togo_october_17_2024() {
        // CDC Gestion comptable §1: LTA24-1710-005 for 17/10/2024 patient #5
        assertThat(InvoiceNumberGenerator.format("LTA", LocalDate.of(2024, 10, 17), 5))
                .isEqualTo("LTA24-1710-005");
    }

    @Test
    void format_zero_pads_seq_to_at_least_three_digits() {
        assertThat(InvoiceNumberGenerator.format("LTA", LocalDate.of(2026, 1, 1), 1))
                .isEqualTo("LTA26-0101-001");
        assertThat(InvoiceNumberGenerator.format("LTA", LocalDate.of(2026, 1, 1), 42))
                .isEqualTo("LTA26-0101-042");
        assertThat(InvoiceNumberGenerator.format("LTA", LocalDate.of(2026, 1, 1), 999))
                .isEqualTo("LTA26-0101-999");
        // beyond 3 digits, grow as needed
        assertThat(InvoiceNumberGenerator.format("LTA", LocalDate.of(2026, 1, 1), 12345))
                .isEqualTo("LTA26-0101-12345");
    }

    @Test
    void format_uses_two_digit_year_mod_100() {
        assertThat(InvoiceNumberGenerator.format("LTA", LocalDate.of(2000, 6, 15), 1))
                .isEqualTo("LTA00-1506-001");
        assertThat(InvoiceNumberGenerator.format("LTA", LocalDate.of(2099, 12, 31), 7))
                .isEqualTo("LTA99-3112-007");
    }
}
