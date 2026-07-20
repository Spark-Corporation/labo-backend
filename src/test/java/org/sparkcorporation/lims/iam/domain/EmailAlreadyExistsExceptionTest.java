package org.sparkcorporation.lims.iam.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailAlreadyExistsExceptionTest {

    @Test
    void message_includes_email() {
        assertThat(new EmailAlreadyExistsException("alice@lab.tg").getMessage())
                .contains("alice@lab.tg");
    }
}
