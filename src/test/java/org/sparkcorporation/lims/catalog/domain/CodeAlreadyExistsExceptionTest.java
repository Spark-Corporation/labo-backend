package org.sparkcorporation.lims.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CodeAlreadyExistsExceptionTest {

    @Test
    void message_includes_entity_name_and_code() {
        var ex = new CodeAlreadyExistsException("discipline", "BIOCHIMIE");
        assertThat(ex.getMessage()).contains("discipline").contains("BIOCHIMIE");
    }
}
