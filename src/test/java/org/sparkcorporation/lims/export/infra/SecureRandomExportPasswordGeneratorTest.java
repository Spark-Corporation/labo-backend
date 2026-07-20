package org.sparkcorporation.lims.export.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SecureRandomExportPasswordGeneratorTest {

    private final SecureRandomExportPasswordGenerator generator =
            new SecureRandomExportPasswordGenerator();

    @Test
    void generates_a_ten_char_password_from_the_safe_alphabet() {
        String pw = generator.generate();
        assertThat(pw).hasSize(10).matches("[0-9A-HJKMNP-TV-Z]{10}");
    }

    @Test
    void successive_passwords_differ() {
        assertThat(generator.generate()).isNotEqualTo(generator.generate());
    }
}
