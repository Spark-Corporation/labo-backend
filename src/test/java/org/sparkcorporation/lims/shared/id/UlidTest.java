package org.sparkcorporation.lims.shared.id;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class UlidTest {

    @Test
    void generate_returns_26_character_string() {
        String id = Ulid.generate();
        assertThat(id).hasSize(26);
    }

    @Test
    void generate_uses_uppercase_Crockford_Base32() {
        String id = Ulid.generate();
        assertThat(id).matches("[0-9A-HJKMNP-TV-Z]{26}");
    }

    @Test
    void generate_produces_unique_values_in_a_batch_of_10000() {
        Set<String> seen = new HashSet<>(10_000);
        for (int i = 0; i < 10_000; i++) {
            assertThat(seen.add(Ulid.generate()))
                    .as("Ulid.generate() collision at iteration " + i)
                    .isTrue();
        }
    }

    @Test
    void generate_produces_lexicographically_sortable_ids_in_time_order()
            throws InterruptedException {
        // Sleep ≥ 5 ms between generations: ulidj uses millisecond timestamp resolution and very
        // fast
        // hardware can occasionally produce two ULIDs in the same millisecond if the gap is too
        // short.
        String first = Ulid.generate();
        Thread.sleep(5);
        String second = Ulid.generate();
        Thread.sleep(5);
        String third = Ulid.generate();
        assertThat(first.compareTo(second)).isLessThanOrEqualTo(0);
        assertThat(second.compareTo(third)).isLessThanOrEqualTo(0);
    }

    @Test
    void isValid_accepts_freshly_generated_id() {
        assertThat(Ulid.isValid(Ulid.generate())).isTrue();
    }

    @Test
    void isValid_accepts_lowercase_form_of_generated_id() {
        String id = Ulid.generate();
        assertThat(Ulid.isValid(id.toLowerCase())).isTrue();
    }

    @Test
    void isValid_rejects_too_short() {
        assertThat(Ulid.isValid("0123456789")).isFalse();
    }

    @Test
    void isValid_rejects_too_long() {
        assertThat(Ulid.isValid("01234567890123456789012345EXTRA")).isFalse();
    }

    @Test
    void isValid_rejects_letter_I() {
        String invalid = "I" + "0123456789ABCDEFGHJKMNPQRS";
        assertThat(Ulid.isValid(invalid)).isFalse();
    }

    @Test
    void isValid_rejects_letter_L() {
        String invalid = "L" + "0123456789ABCDEFGHJKMNPQRS";
        assertThat(Ulid.isValid(invalid)).isFalse();
    }

    @Test
    void isValid_rejects_letter_O() {
        String invalid = "O" + "0123456789ABCDEFGHJKMNPQRS";
        assertThat(Ulid.isValid(invalid)).isFalse();
    }

    @Test
    void isValid_rejects_letter_U() {
        String invalid = "U" + "0123456789ABCDEFGHJKMNPQRS";
        assertThat(Ulid.isValid(invalid)).isFalse();
    }

    @Test
    void isValid_rejects_null_safely() {
        assertThat(Ulid.isValid(null)).isFalse();
    }
}
