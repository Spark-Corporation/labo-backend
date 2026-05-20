package org.sparkcorporation.lims.cashbox.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ExpenseCategoryIdTest {

    @Test
    void rejectsNullAndInvalidUlid() {
        assertThatThrownBy(() -> new ExpenseCategoryId(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ExpenseCategoryId("not-a-ulid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid ULID");
    }

    @Test
    void generateProducesValidUlid() {
        var id = ExpenseCategoryId.generate();
        assertThat(id.value()).hasSize(26);
    }
}
