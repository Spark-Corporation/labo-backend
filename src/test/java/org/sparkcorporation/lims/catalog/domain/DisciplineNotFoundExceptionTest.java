package org.sparkcorporation.lims.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.DisciplineId;

class DisciplineNotFoundExceptionTest {

    @Test
    void is_an_EntityNotFoundException() {
        assertThat(new DisciplineNotFoundException(DisciplineId.next()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void message_includes_id() {
        var id = DisciplineId.next();
        assertThat(new DisciplineNotFoundException(id).getMessage()).contains(id.value());
    }
}
