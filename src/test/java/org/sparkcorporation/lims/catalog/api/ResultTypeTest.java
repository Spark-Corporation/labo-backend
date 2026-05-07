package org.sparkcorporation.lims.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ResultTypeTest {

    @Test
    void has_five_values_in_declared_order() {
        assertThat(ResultType.values())
                .containsExactly(
                        ResultType.FORMULA,
                        ResultType.INTEGER,
                        ResultType.REAL,
                        ResultType.TEXT,
                        ResultType.FREE);
    }
}
